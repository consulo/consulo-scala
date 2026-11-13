package org.jetbrains.jps.incremental.scala.shared;

import consulo.component.util.localize.AbstractBundle;
import consulo.logging.Logger;
import consulo.util.lang.reflect.ReflectionUtil;
import org.jetbrains.annotations.NonNls;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ResourceBundle;

// upstream: @ApiStatus.Internal
public abstract class AbstractScalaDynamicBundle extends AbstractBundle {

    protected AbstractScalaDynamicBundle(@NonNls String pathToBundle) {
        super(pathToBundle);
    }

    /**
     * The following code was copied from org.jetbrains.jps.api.JpsDynamicBundle, to avoid linking against it.
     */

    private static final String LANGUAGE_BUNDLE = "jps.language.bundle";

    private static final Logger LOG = Logger.getInstance(AbstractScalaDynamicBundle.class);

    private static final @Nullable Method SET_PARENT = getSetParentMethod();
    private static final @Nullable ClassLoader ourLangBundleLoader;
    static {
        ClassLoader loader = null;
        try {
            final String bundlePath = System.getProperty(LANGUAGE_BUNDLE, null);
            if (bundlePath != null) {
                loader = new URLClassLoader(new URL[] {Paths.get(bundlePath).toUri().toURL()}, null);
            }
        }
        catch (Throwable e) {
            LOG.info(e);
        }
        finally {
            ourLangBundleLoader = loader;
        }
    }

    @SuppressWarnings("deprecation")
    private static @Nullable Method getSetParentMethod() {
        try {
            return ReflectionUtil.getDeclaredMethod(ResourceBundle.class, "setParent", ResourceBundle.class);
        }
        catch (Throwable e) {
            return null;
        }
    }

    // Consulo: there is no JPS build process and AbstractBundle has no findBundle(String, ClassLoader, ResourceBundle.Control)
    // hook (localization is done by LocalizeValue), so language bundle support of upstream is not applicable:
    // /**
    //  * A base class for resource bundles used in JPS build process
    //  * This class provides support for "dynamic" resource bundles provided by {@link com.intellij.DynamicBundle.LanguageBundleEP} extension.
    //  * if JPS plugin inherits the ResourceBundle from this class, IDE's language pack's localized resources will be automatically available for the JPS process launched by the IDE.
    //  */
    //
    // @Override
    // protected ResourceBundle findBundle(
    //         @NonNls String pathToBundle,
    //         ClassLoader loader,
    //         ResourceBundle.Control control
    // ) {
    //     final ResourceBundle base = super.findBundle(pathToBundle, loader, control);
    //     final ClassLoader languageBundleLoader = ourLangBundleLoader;
    //     if (languageBundleLoader != null) {
    //         ResourceBundle languageBundle = super.findBundle(pathToBundle, languageBundleLoader, control);
    //         try {
    //             if (SET_PARENT != null) {
    //                 SET_PARENT.invoke(languageBundle, base);
    //             }
    //             return languageBundle;
    //         }
    //         catch (Throwable e) {
    //             LOG.warn(e);
    //         }
    //     }
    //
    //     return base;
    // }
}
