package in.naveens.mqttbroker.service;

import android.util.Log;

import java.io.IOException;
import java.net.BindException;
import java.util.Properties;
import java.util.concurrent.Callable;

import io.moquette.broker.Server;

public class MQTTBroker implements Callable<Boolean> {

    private static final String TAG = MQTTBroker.class.getName();

    private static Server server;
    private Properties config;

    public MQTTBroker(Properties config) {
        this.config = config;
    }

    public static Server getServer() {
        return server;
    }

    public void stopServer() {
        server.stopServer();
    }

    @Override
    public Boolean call() throws BindException {

        try {
            server = ServerInstance.getServerInstance();
            server.startServer(config);
            Log.d(TAG, "Serwer işe girizildi");
            return true;
        } catch (BindException e) {
            Log.e(TAG, "Salgysy eýýäm ulanylýar. Baglanyp bolmaýar.");
            Log.e(TAG, "Ýalňyş : " + e.getMessage());
            throw new BindException(e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, "Ýalňyş : " + e.getMessage());
        }
        return false;
    }
}
