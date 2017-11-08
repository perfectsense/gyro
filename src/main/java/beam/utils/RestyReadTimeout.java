package beam.utils;

import us.monoid.web.Resty.Option;

import java.net.URLConnection;

public class RestyReadTimeout extends Option {

    private int readTimeout;

    public RestyReadTimeout(int t) {
        readTimeout = t;
    }

    @Override
    public void apply(URLConnection connection) {
        connection.setReadTimeout(readTimeout);
    }

}
