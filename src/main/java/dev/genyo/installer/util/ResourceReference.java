package dev.genyo.installer.util;

import java.util.Objects;

/*
 * Used to store these paths, so I don't have to type it over and over.
 */
public final class ResourceReference {

    public static final String STYLE_RES = Objects.requireNonNull(
            ResourceReference.class.getResource("/style/style.css")).toExternalForm();
    public static final String IMG512_RES = Objects.requireNonNull(
            ResourceReference.class.getResource("/images/genyo512.png")).toExternalForm();

}
