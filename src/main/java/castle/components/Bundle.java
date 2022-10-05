package castle.components;

import arc.struct.ObjectMap;
import arc.struct.Seq;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static mindustry.Vars.mods;

public class Bundle {

    public static final Locale defaultLocale = new Locale("en");
    public static final Seq<Locale> supportedLocales = new Seq<>();

    private static final ObjectMap<Locale, ResourceBundle> bundles = new ObjectMap<>();
    private static final ObjectMap<Locale, MessageFormat> formats = new ObjectMap<>();

    public static void load() {
        var files = mods.getMod("castle-wars").root.child("bundles").seq();

        files.each(file -> {
            var codes = file.nameWithoutExtension().split("_");

            if (codes.length == 1) { // bundle.properties
                supportedLocales.add(Locale.ROOT);
            } else if (codes.length == 2) { // bundle_ru.properties
                supportedLocales.add(new Locale(codes[1]));
            } else if (codes.length == 3) { // bundle_uk_UA.properties
                supportedLocales.add(new Locale(codes[1], codes[2]));
            }
        });

        supportedLocales.each(locale -> {
            bundles.put(locale, ResourceBundle.getBundle("bundles.bundle", locale));
            formats.put(locale, new MessageFormat("", locale));
        });
    }

    public static String get(String key, String defaultValue, Locale locale) {
        try {
            var bundle = bundles.get(locale, bundles.get(defaultLocale));
            return bundle.getString(key);
        } catch (MissingResourceException ignored) {
            return defaultValue;
        }
    }

    public static String get(String key, Locale locale) {
        return get(key, key, locale);
    }

    public static String format(String key, Locale locale, Object... values) {
        var pattern = get(key, locale);
        if (values.length == 0) {
            return pattern;
        }

        var format = formats.get(locale, formats.get(defaultLocale));
        format.applyPattern(pattern);
        return format.format(values);
    }

    public static String format(String key, Object... values) {
        return format(key, defaultLocale, values);
    }
}
