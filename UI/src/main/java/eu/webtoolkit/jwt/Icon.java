package eu.webtoolkit.jwt;

/**
 * Utility class to gain access to WIcon, which is non-public
 */
public class Icon extends WIcon {
    public Icon(WContainerWidget parent) {
        super(parent);
    }

    public Icon() {
        super();
    }

    public Icon(final String name, WContainerWidget parent) {
        super(name, parent);
    }

    public Icon(final String name) {
        super(name);
    }
}
