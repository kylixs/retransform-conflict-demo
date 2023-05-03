package net.bytebuddy.dynamic;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.utility.nullability.AlwaysNull;
import net.bytebuddy.agent.utility.nullability.MaybeNull;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static net.bytebuddy.matcher.ElementMatchers.isChildOf;

public class SWClassFileLocatorForInstrumentation implements ClassFileLocator {

        /**
         * A dispatcher for interacting with the instrumentation API.
         */
        private static final Dispatcher DISPATCHER = doPrivileged(JavaDispatcher.of(Dispatcher.class));

        /**
         * The instrumentation instance to use for looking up the binary format of a type.
         */
        private final Instrumentation instrumentation;

        /**
         * The delegate to load a class by its name.
         */
        private final ClassLoadingDelegate classLoadingDelegate;

        /**
         * Creates an agent-based class file locator.
         *
         * @param instrumentation The instrumentation to be used.
         * @param classLoader     The class loader to read a class from or {@code null} to use the boot loader.
         */
        public SWClassFileLocatorForInstrumentation(Instrumentation instrumentation, @MaybeNull ClassLoader classLoader) {
            this(instrumentation, ClassLoadingDelegate.Default.of(classLoader));
        }

        /**
         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
         *
         * @param action The action to execute from a privileged context.
         * @param <T>    The type of the action's resolved value.
         * @return The action's resolved value.
         */
        @AccessControllerPlugin.Enhance
        private static <T> T doPrivileged(PrivilegedAction<T> action) {
            return action.run();
        }

        /**
         * Creates an agent-based class file locator.
         *
         * @param instrumentation      The instrumentation to be used.
         * @param classLoadingDelegate The delegate responsible for class loading.
         */
        public SWClassFileLocatorForInstrumentation(Instrumentation instrumentation, ClassLoadingDelegate classLoadingDelegate) {
            if (!DISPATCHER.isRetransformClassesSupported(instrumentation)) {
                throw new IllegalArgumentException(instrumentation + " does not support retransformation");
            }
            this.instrumentation = instrumentation;
            this.classLoadingDelegate = classLoadingDelegate;
        }

        /**
         * Resolves the instrumentation provided by {@code net.bytebuddy.agent.Installer}.
         *
         * @return The installed instrumentation instance.
         */
        private static Instrumentation resolveByteBuddyAgentInstrumentation() {
            try {
                Class<?> installer = ClassLoader.getSystemClassLoader().loadClass("net.bytebuddy.agent.Installer");
                JavaModule source = JavaModule.ofType(AgentBuilder.class), target = JavaModule.ofType(installer);
                if (source != null && !source.canRead(target)) {
                    Class<?> module = Class.forName("java.lang.Module");
                    module.getMethod("addReads", module).invoke(source.unwrap(), target.unwrap());
                }
                return (Instrumentation) installer.getMethod("getInstrumentation").invoke(null);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", exception);
            }
        }

        /**
         * Returns an agent-based class file locator for the given class loader and an already installed
         * Byte Buddy-agent.
         *
         * @param classLoader The class loader that is expected to load the looked-up a class.
         * @return A class file locator for the given class loader based on a Byte Buddy agent.
         */
        public static ClassFileLocator fromInstalledAgent(@MaybeNull ClassLoader classLoader) {
            return new SWClassFileLocatorForInstrumentation(resolveByteBuddyAgentInstrumentation(), classLoader);
        }

        /**
         * Returns a class file locator that is capable of locating a class file for the given type using the given instrumentation instance.
         *
         * @param instrumentation The instrumentation instance to query for a retransformation.
         * @param type            The locatable type which class loader is used as a fallback.
         * @return A class file locator for locating the class file of the given type.
         */
        public static ClassFileLocator of(Instrumentation instrumentation, Class<?> type) {
            return new SWClassFileLocatorForInstrumentation(instrumentation, ClassLoadingDelegate.Explicit.of(type));
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) {
            try {
                ExtractionClassFileTransformer classFileTransformer = new ExtractionClassFileTransformer(classLoadingDelegate.getClassLoader(), name);
                //DISPATCHER.addTransformer(instrumentation, classFileTransformer, true);
                instrumentation.addTransformer(classFileTransformer, true);
                try {
                  //  DISPATCHER.retransformClasses(instrumentation, new Class<?>[]{classLoadingDelegate.locate(name)});
                    instrumentation.retransformClasses(new Class<?>[]{classLoadingDelegate.locate(name)});
                    byte[] binaryRepresentation = classFileTransformer.getBinaryRepresentation();
                    return binaryRepresentation == null
                            ? new Resolution.Illegal(name)
                            : new Resolution.Explicit(binaryRepresentation);
                } finally {
                    instrumentation.removeTransformer(classFileTransformer);
                }
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception ignored) {
                return new Resolution.Illegal(name);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }

        /**
         * A dispatcher to interact with the {@link Instrumentation} API.
         */
        @JavaDispatcher.Proxied("java.lang.instrument.Instrumentation")
        protected interface Dispatcher {

            /**
             * Invokes the {@code Instrumentation#isRetransformClassesSupported} method.
             *
             * @param instrumentation The instrumentation instance to invoke the method on.
             * @return {@code true} if the supplied instrumentation instance supports retransformation.
             */
            boolean isRetransformClassesSupported(Instrumentation instrumentation);

            /**
             * Registers a transformer.
             *
             * @param instrumentation      The instrumentation instance to invoke the method on.
             * @param classFileTransformer The class file transformer to register.
             * @param canRetransform       {@code true} if the class file transformer should be invoked upon a retransformation.
             */
            void addTransformer(Instrumentation instrumentation, ClassFileTransformer classFileTransformer, boolean canRetransform);

            /**
             * Retransforms the supplied classes.
             *
             * @param instrumentation The instrumentation instance to invoke the method on.
             * @param type            The types to retransform.
             * @throws UnmodifiableClassException If any of the supplied types are unmodifiable.
             */
            void retransformClasses(Instrumentation instrumentation, Class<?>[] type) throws UnmodifiableClassException;
        }

        /**
         * A delegate that is queried for loading a class.
         */
        public interface ClassLoadingDelegate {

            /**
             * Loads a class by its name.
             *
             * @param name The name of the type.
             * @return The class with the given name.
             * @throws ClassNotFoundException If a class cannot be found.
             */
            Class<?> locate(String name) throws ClassNotFoundException;

            /**
             * Returns the underlying class loader.
             *
             * @return The underlying class loader.
             */
            @MaybeNull
            ClassLoader getClassLoader();

            /**
             * A default implementation of a class loading delegate.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Default implements ClassLoadingDelegate {

                /**
                 * A class loader that does not define resources of its own but allows querying for resources supplied by the boot loader.
                 */
                private static final ClassLoader BOOT_LOADER_PROXY = doPrivileged(BootLoaderProxyCreationAction.INSTANCE);

                /**
                 * The underlying class loader.
                 */
                protected final ClassLoader classLoader;

                /**
                 * Creates a default class loading delegate.
                 *
                 * @param classLoader The class loader to be queried.
                 */
                protected Default(ClassLoader classLoader) {
                    this.classLoader = classLoader;
                }

                /**
                 * Creates a class loading delegate for the given class loader.
                 *
                 * @param classLoader The class loader for which to create a delegate or {@code null} to use the boot loader.
                 * @return The class loading delegate for the provided class loader.
                 */
                public static ClassLoadingDelegate of(@MaybeNull ClassLoader classLoader) {
                    return ForDelegatingClassLoader.isDelegating(classLoader)
                            ? new ForDelegatingClassLoader(classLoader)
                            : new Default(classLoader == null ? BOOT_LOADER_PROXY : classLoader);
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> locate(String name) throws ClassNotFoundException {
                    return classLoader.loadClass(name);
                }

                /**
                 * {@inheritDoc}
                 */
                @MaybeNull
                public ClassLoader getClassLoader() {
                    return classLoader == BOOT_LOADER_PROXY
                            ? ClassLoadingStrategy.BOOTSTRAP_LOADER
                            : classLoader;
                }

                /**
                 * A privileged action for creating a proxy class loader for the boot class loader.
                 */
                protected enum BootLoaderProxyCreationAction implements PrivilegedAction<ClassLoader> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public ClassLoader run() {
                        return new URLClassLoader(new URL[0], ClassLoadingStrategy.BOOTSTRAP_LOADER);
                    }
                }
            }

            /**
             * A class loading delegate that accounts for a {@code sun.reflect.DelegatingClassLoader} which
             * cannot load its own classes by name.
             */
            class ForDelegatingClassLoader extends Default {

                /**
                 * The name of the delegating class loader.
                 */
                private static final String DELEGATING_CLASS_LOADER_NAME = "sun.reflect.DelegatingClassLoader";

                /**
                 * An index indicating the first element of a collection.
                 */
                private static final int ONLY = 0;

                /**
                 * A dispatcher for extracting a class loader's loaded classes.
                 */
                private static final Dispatcher.Initializable DISPATCHER = doPrivileged(Dispatcher.CreationAction.INSTANCE);

                /**
                 * Creates a class loading delegate for a delegating class loader.
                 *
                 * @param classLoader The delegating class loader.
                 */
                protected ForDelegatingClassLoader(ClassLoader classLoader) {
                    super(classLoader);
                }

                /**
                 * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
                 *
                 * @param action The action to execute from a privileged context.
                 * @param <T>    The type of the action's resolved value.
                 * @return The action's resolved value.
                 */
                @AccessControllerPlugin.Enhance
                private static <T> T doPrivileged(PrivilegedAction<T> action) {
                    return action.run();
                }

                /**
                 * Checks if a class loader is a delegating class loader.
                 *
                 * @param classLoader The class loader to inspect or {@code null} to check the boot loader.
                 * @return {@code true} if the class loader is a delegating class loader.
                 */
                protected static boolean isDelegating(@MaybeNull ClassLoader classLoader) {
                    return classLoader != null && classLoader.getClass().getName().equals(DELEGATING_CLASS_LOADER_NAME);
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> locate(String name) throws ClassNotFoundException {
                    Vector<Class<?>> classes;
                    try {
                        classes = DISPATCHER.initialize().extract(classLoader);
                    } catch (RuntimeException ignored) {
                        return super.locate(name);
                    }
                    if (classes.size() != 1) {
                        return super.locate(name);
                    }
                    Class<?> type = classes.get(ONLY);
                    return TypeDescription.ForLoadedType.getName(type).equals(name)
                            ? type
                            : super.locate(name);
                }

                /**
                 * Representation of a Java {@link java.lang.reflect.Field}.
                 */
                protected interface Dispatcher {

                    /**
                     * Reads the classes of the represented collection.
                     *
                     * @param classLoader The class loader to read from.
                     * @return The class loader's loaded classes.
                     */
                    Vector<Class<?>> extract(ClassLoader classLoader);

                    /**
                     * An uninitialized version of a dispatcher for extracting a class loader's loaded classes.
                     */
                    interface Initializable {

                        /**
                         * Initializes the dispatcher.
                         *
                         * @return An initialized dispatcher.
                         */
                        Dispatcher initialize();
                    }

                    /**
                     * An action for creating a dispatcher.
                     */
                    enum CreationAction implements PrivilegedAction<Initializable> {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        /**
                         * {@inheritDoc}
                         */
                        public Initializable run() {
                            try {
                                return new Dispatcher.Resolved(ClassLoader.class.getDeclaredField("classes"));
                            } catch (Exception exception) {
                                return new Dispatcher.Unresolved(exception.getMessage());
                            }
                        }
                    }

                    /**
                     * Represents a field that could be located.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    class Resolved implements Dispatcher, Initializable, PrivilegedAction<Dispatcher> {

                        /**
                         * The represented field.
                         */
                        private final Field field;

                        /**
                         * Creates a new resolved field.
                         *
                         * @param field the represented field.l
                         */
                        public Resolved(Field field) {
                            this.field = field;
                        }

                        /**
                         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
                         *
                         * @param action The action to execute from a privileged context.
                         * @param <T>    The type of the action's resolved value.
                         * @return The action's resolved value.
                         */
                        @AccessControllerPlugin.Enhance
                        private static <T> T doPrivileged(PrivilegedAction<T> action) {
                            return action.run();
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Dispatcher initialize() {
                            return doPrivileged(this);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        @SuppressWarnings("unchecked")
                        public Vector<Class<?>> extract(ClassLoader classLoader) {
                            try {
                                return (Vector<Class<?>>) field.get(classLoader);
                            } catch (IllegalAccessException exception) {
                                throw new IllegalStateException("Cannot access field", exception);
                            }
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Dispatcher run() {
                            field.setAccessible(true);
                            return this;
                        }
                    }

                    /**
                     * Represents a field that could not be located.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    class Unresolved implements Initializable {

                        /**
                         * The reason why this dispatcher is unavailable.
                         */
                        private final String message;

                        /**
                         * Creates a representation of a non-resolved field.
                         *
                         * @param message The reason why this dispatcher is unavailable.
                         */
                        public Unresolved(String message) {
                            this.message = message;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Dispatcher initialize() {
                            throw new UnsupportedOperationException("Could not locate classes vector: " + message);
                        }
                    }
                }
            }

            /**
             * A class loading delegate that allows the location of explicitly registered classes that cannot
             * be located by a class loader directly. This allows for locating classes that are loaded by
             * an anonymous class loader which does not register its classes in a system dictionary.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Explicit implements ClassLoadingDelegate {

                /**
                 * A class loading delegate that is queried for classes that are not registered explicitly.
                 */
                private final ClassLoadingDelegate fallbackDelegate;

                /**
                 * The map of registered classes mapped by their name.
                 */
                private final Map<String, Class<?>> types;

                /**
                 * Creates a new class loading delegate with a possibility of looking up explicitly
                 * registered classes.
                 *
                 * @param classLoader The class loader to be used for looking up classes.
                 * @param types       A collection of classes that cannot be looked up explicitly.
                 */
                public Explicit(@MaybeNull ClassLoader classLoader, Collection<? extends Class<?>> types) {
                    this(Default.of(classLoader), types);
                }

                /**
                 * Creates a new class loading delegate with a possibility of looking up explicitly
                 * registered classes.
                 *
                 * @param fallbackDelegate The class loading delegate to query for any class that is not
                 *                         registered explicitly.
                 * @param types            A collection of classes that cannot be looked up explicitly.
                 */
                public Explicit(ClassLoadingDelegate fallbackDelegate, Collection<? extends Class<?>> types) {
                    this.fallbackDelegate = fallbackDelegate;
                    this.types = new HashMap<String, Class<?>>();
                    for (Class<?> type : types) {
                        this.types.put(TypeDescription.ForLoadedType.getName(type), type);
                    }
                }

                /**
                 * Creates an explicit class loading delegate for the given type.
                 *
                 * @param type The type that is explicitly locatable.
                 * @return A suitable class loading delegate.
                 */
                public static ClassLoadingDelegate of(Class<?> type) {
                    return new Explicit(type.getClassLoader(), Collections.singleton(type));
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> locate(String name) throws ClassNotFoundException {
                    Class<?> type = types.get(name);
                    return type == null
                            ? fallbackDelegate.locate(name)
                            : type;
                }

                /**
                 * {@inheritDoc}
                 */
                @MaybeNull
                public ClassLoader getClassLoader() {
                    return fallbackDelegate.getClassLoader();
                }
            }
        }

        /**
         * A non-operational class file transformer that remembers the binary format of a given class.
         */
        protected static class ExtractionClassFileTransformer implements ClassFileTransformer {

            /**
             * An indicator that an attempted class file transformation did not alter the handed class file.
             */
            @AlwaysNull
            private static final byte[] DO_NOT_TRANSFORM = null;

            /**
             * The class loader that is expected to have loaded the looked-up a class.
             */
            @MaybeNull
            private final ClassLoader classLoader;

            /**
             * The name of the type to look up.
             */
            private final String typeName;

            /**
             * The binary representation of the looked-up class.
             */
            @MaybeNull
            //@SuppressFBWarnings(value = "VO_VOLATILE_REFERENCE_TO_ARRAY", justification = "The array is not to be modified by contract")
            private volatile byte[] binaryRepresentation;

            /**
             * Creates a class file transformer for the purpose of extraction.
             *
             * @param classLoader The class loader that is expected to have loaded the looked-up a class.
             * @param typeName    The name of the type to look up.
             */
            protected ExtractionClassFileTransformer(@MaybeNull ClassLoader classLoader, String typeName) {
                this.classLoader = classLoader;
                this.typeName = typeName;
            }

            /**
             * {@inheritDoc}
             */
            @MaybeNull
            //@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "The array is not modified by class contract.")
            public byte[] transform(@MaybeNull ClassLoader classLoader,
                                    @MaybeNull String internalName,
                                    @MaybeNull Class<?> redefinedType,
                                    ProtectionDomain protectionDomain,
                                    byte[] binaryRepresentation) {
                if (internalName != null && isChildOf(this.classLoader).matches(classLoader) && typeName.equals(internalName.replace('/', '.'))) {
                    this.binaryRepresentation = binaryRepresentation.clone();
                }
                return DO_NOT_TRANSFORM;
            }

            /**
             * Returns the binary representation of the class file that was looked up. The returned array must never be modified.
             *
             * @return The binary representation of the class file or {@code null} if no such class file could
             * be located.
             */
            @MaybeNull
            //@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The array is not to be modified by contract")
            protected byte[] getBinaryRepresentation() {
                return binaryRepresentation;
            }
        }
    }
