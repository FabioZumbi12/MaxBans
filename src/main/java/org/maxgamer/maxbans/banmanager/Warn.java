package org.maxgamer.maxbans.banmanager;

public class Warn {
    private final String reason;
    private final String banner;
    private final long expires;
    
    public Warn(final String reason, final String banner, final long expires) {
        super();
        this.reason = reason;
        this.banner = banner;
        this.expires = expires;
    }
    
    public String getReason() {
        return this.reason;
    }
    
    public String getBanner() {
        return this.banner;
    }
    
    public long getExpires() {
        return this.expires;
    }
}
