package org.ulyssis.ipp.utils;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Helper classes to make using Jedis a bit more practical.
 */
public final class JedisHelper {

    // Static methods only! No instances!
    private JedisHelper() {
    }

    /**
     * Get a new Jedis instance from the given URI
     *
     * The URI is formed as such:
     * `new URI("redis://:password@host:port/database")`
     * for example:
     * `new URI("redis://:hunter2@10.0.0.1:6379/0")}`.
     * The port, password and path can be omitted. If no path is
     * supplied, database +0+ is selected. If no user
     * info is supplied, then no authorization is performed.
     * If no port is supplied, the default port +6379+
     * is used.
     *
     * Use this instead of `new Jedis(uri)`, because
     * that method does not allow to omit anything.
     *
     * @param uri
     *        The URI for the Jedis instance.
     * @return A new Jedis instance.
     */
    // TODO: Exceptions for wrong password, failing to parse database,...
    public static Jedis get(URI uri) throws IllegalArgumentException {
        int port = uri.getPort() != -1 ? uri.getPort() : 6379;
        String host = uri.getHost();
        Jedis result = new Jedis(host, port);
        if (uri.getUserInfo() != null) {
            String password = uri.getUserInfo().split(":")[1];
            result.auth(password);
        }
        result.select(getDb(uri));
        return result;
    }

    /**
     * Extract the database number from the given Redis URI
     *
     * If the URI has the path +/N+, then +N+
     * is returned. If the path is empty, +0+ is returned.
     *
     * @param uri
     *        The URI
     * @throws java.lang.IllegalArgumentException
     *         The supplied path could not be parsed as an int
     * @return The database number (+0+ for no path, +N+
     *         if the path is +/N+)
     */
    public static int getDb(URI uri) throws IllegalArgumentException {
        if (!Objects.equals(uri.getPath(), "")) {
            try {
                return Integer.parseInt(uri.getPath().split("/", 2)[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("The supplied path could not be parsed as an int", e);
            }
        }
        return 0;
    }

    /**
     * Generates a channel name that is local to a database.
     *
     * Redis channels are not tied to a single database, but are
     * global to the instance of Redis. We, however, want to tie
     * these to a certain database, so we have to add some information
     * that does that. For this reason, if the channel name is
     * +channel+, the database number +N+ is added to it
     * to form +channel:N+. The database number is extracted
     * using `getDb(uri)`.
     *
     * @param channel
     *        The base channel name
     * @param uri
     *        The URI containing the database info
     * @return +channel:N+, where +N+ is the database number
     * @see org.ulyssis.ipp.utils.JedisHelper#getDb(java.net.URI)
     */
    public static String dbLocalChannel(String channel, URI uri) {
        return channel + ":" + getDb(uri);
    }

    /**
     * A helper class that implements +BinaryJedisPubSub+ using callbacks
     * that can be registered.
     *
     * Instead of having to implement six methods where you probably
     * only need one, you only need to supply callbacks for the ones you're
     * interested in. This becomes extra useful when using Java 8, because
     * function references or lambda functions can be used.
     *
     * Adding a callback is thread safe. +CopyOnWriteArrayList+ is used,
     * because this class is optimized for frequent calling of callbacks,
     * not for registering callback functions.
     *
     * All callbacks are called from the thread currently blocking on
     * a +subscribe()+ (or similar) call.
     *
     * Currently, callbacks can only be added, not removed.
     */
    public static class BinaryCallBackPubSub extends BinaryJedisPubSub {
        /**
         * This is necessary, because there is no triconsumer.
         */
        @FunctionalInterface
        public interface OnPMessageCallback {
            public void accept(byte[] pattern, byte[] channel, byte[] message);
        }

        private final List<BiConsumer<byte[],byte[]>> onMessageCallbacks
                = new CopyOnWriteArrayList<>();
        private final List<OnPMessageCallback> onPMessageCallbacks
                = new CopyOnWriteArrayList<>();
        private final List<BiConsumer<byte[],Integer>> onSubscribeCallbacks
                = new CopyOnWriteArrayList<>();
        private final List<BiConsumer<byte[],Integer>> onUnsubscribeCallbacks
                = new CopyOnWriteArrayList<>();
        private final List<BiConsumer<byte[],Integer>> onPSubscribeCallbacks
                = new CopyOnWriteArrayList<>();
        private final List<BiConsumer<byte[],Integer>> onPUnsubscribeCallbacks
                = new CopyOnWriteArrayList<>();

        /**
         * Implementation of +onMessage+. Forwards the call to the registered callbacks.
         *
         * @see org.ulyssis.ipp.utils.JedisHelper.BinaryCallBackPubSub#addOnMessageListener(java.util.function.BiConsumer)
         */
        @Override
        public void onMessage(byte[] channel, byte[] message) {
            onMessageCallbacks.stream().forEach(callback -> callback.accept(channel, message));
        }

        /**
         * Add a callback for +onMessage+.
         *
         * @param callback
         *        The callback to add. The first argument is the
         *        channel, the second is the message.
         * @see org.ulyssis.ipp.utils.JedisHelper.BinaryCallBackPubSub#onMessage(byte[], byte[])
         */
        public void addOnMessageListener(BiConsumer<byte[],byte[]> callback) {
            onMessageCallbacks.add(callback);
        }

        /**
         * Implementation of +onPMessage+. Forwards the call to the registered callbacks.
         *
         * @see org.ulyssis.ipp.utils.JedisHelper.BinaryCallBackPubSub#addOnPMessageListener(org.ulyssis.ipp.utils.JedisHelper.BinaryCallBackPubSub.OnPMessageCallback)
         */
        @Override
        public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
            onPMessageCallbacks.stream().forEach(callback -> callback.accept(pattern, channel, message));
        }

        /**
         * Add a callback for +onPMessage+.
         *
         * @param callback
         *        The callback to add. The first argument is the
         *        pattern is the channel, the second is the channel,
         *        and the third is the message.
         * @see org.ulyssis.ipp.utils.JedisHelper.BinaryCallBackPubSub#onPMessage(byte[], byte[], byte[])
         */
        public void addOnPMessageListener(OnPMessageCallback callback) {
            onPMessageCallbacks.add(callback);
        }

        /**
         * Implementation of +onSubscribe+. Forwards the call to the registered callbacks.
         *
         * @see org.ulyssis.ipp.utils.JedisHelper.BinaryCallBackPubSub#addOnSubscribeListener(java.util.function.BiConsumer)
         */
        @Override
        public void onSubscribe(byte[] channel, int subscribedChannels) {
            onSubscribeCallbacks.stream().forEach(callback -> callback.accept(channel, subscribedChannels));
        }

        /**
         * Add a callback for +onSubscribe+
         *
         * @param callback
         *        The callback to add. The first argument is the channel,
         *        the second is the number of subscribed channels.
         * @see org.ulyssis.ipp.utils.JedisHelper.BinaryCallBackPubSub#onSubscribe(byte[], int)
         */
        public void addOnSubscribeListener(BiConsumer<byte[], Integer> callback) {
            onSubscribeCallbacks.add(callback);
        }

        /**
         * Implementation of +onUnsubscribe+. Forwards the call to the registered callbacks.
         *
         * @see org.ulyssis.ipp.utils.JedisHelper.BinaryCallBackPubSub#addOnUnsubscribeListener(java.util.function.BiConsumer)
         */
        @Override
        public void onUnsubscribe(byte[] channel, int subscribedChannels) {
            onUnsubscribeCallbacks.stream().forEach(callback -> callback.accept(channel, subscribedChannels));
        }

        /**
         * Add a callback for +onUnsubscribe+
         *
         * @param callback
         *        The callback to add. The first argument is the channel,
         *        the second is the number of subscribed channels.
         * @see org.ulyssis.ipp.utils.JedisHelper.BinaryCallBackPubSub#onUnsubscribe(byte[], int)
         */
        public void addOnUnsubscribeListener(BiConsumer<byte[], Integer> callback) {
            onUnsubscribeCallbacks.add(callback);
        }

        /**
         * Implementation of +onPSubscribe+. Forwards the call to the registered callbacks.
         *
         * @see org.ulyssis.ipp.utils.JedisHelper.BinaryCallBackPubSub#addOnPSubscribeListener(java.util.function.BiConsumer)
         */
        @Override
        public void onPSubscribe(byte[] pattern, int subscribedChannels) {
            onPSubscribeCallbacks.stream().forEach(callback -> callback.accept(pattern, subscribedChannels));
        }

        /**
         * Add a callback for +onPSubscribe+
         *
         * @param callback
         *        The callback to add. The first argument is the pattern,
         *        the second is the number of subscribed channels.
         * @see org.ulyssis.ipp.utils.JedisHelper.BinaryCallBackPubSub#onPSubscribe(byte[], int)
         */
        public void addOnPSubscribeListener(BiConsumer<byte[], Integer> callback) {
            onPSubscribeCallbacks.add(callback);
        }

        /**
         * Implementation of +onPUnsubscribe+. Forwards the call to the registered callbacks.
         *
         * @see org.ulyssis.ipp.utils.JedisHelper.BinaryCallBackPubSub#addOnPUnsubscribeListener(java.util.function.BiConsumer)
         */
        @Override
        public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
            onPUnsubscribeCallbacks.stream().forEach(callback -> callback.accept(pattern, subscribedChannels));
        }

        /**
         * Add a callback for +onPUnsubscribe+
         *
         * @param callback
         *        The callback to add. The first argument is the pattern,
         *        the second is the number of subscribed channels.
         * @see org.ulyssis.ipp.utils.JedisHelper.BinaryCallBackPubSub#onPUnsubscribe(byte[], int)
         */
        public void addOnPUnsubscribeListener(BiConsumer<byte[], Integer> callback) {
            onPUnsubscribeCallbacks.add(callback);
        }
    }

    /**
     * A helper class that implements +JedisPubSub+ using callbacks
     * that can be registered.
     *
     * Instead of having to implement six methods where you probably
     * only need one, you only need to supply callbacks for the ones you're
     * interested in. This becomes extra useful when using Java 8, because
     * function references or lambda functions can be used.
     *
     * Adding a callback is thread safe. +CopyOnWriteArrayList+ is used,
     * because this class is optimized for frequent calling of callbacks,
     * not for registering callback functions.
     *
     * All callbacks are called from the thread currently blocking on
     * a +subscribe()+ (or similar) call.
     *
     * Currently, callbacks can only be added, not removed.
     */
    public static class CallBackPubSub extends JedisPubSub {
        /**
         * This is necessary, because there is no triconsumer.
         */
        @FunctionalInterface
        public interface OnPMessageCallback {
            public void accept(String pattern, String channel, String message);
        }

        private final List<BiConsumer<String,String>> onMessageCallbacks
                = new CopyOnWriteArrayList<>();
        private final List<OnPMessageCallback> onPMessageCallbacks
                = new CopyOnWriteArrayList<>();
        private final List<BiConsumer<String,Integer>> onSubscribeCallbacks
                = new CopyOnWriteArrayList<>();
        private final List<BiConsumer<String,Integer>> onUnsubscribeCallbacks
                = new CopyOnWriteArrayList<>();
        private final List<BiConsumer<String,Integer>> onPSubscribeCallbacks
                = new CopyOnWriteArrayList<>();
        private final List<BiConsumer<String,Integer>> onPUnsubscribeCallbacks
                = new CopyOnWriteArrayList<>();

        /**
         * Implementation of +onMessage+. Forwards the call to the registered callbacks.
         *
         * @see org.ulyssis.ipp.utils.JedisHelper.CallBackPubSub#addOnMessageListener(java.util.function.BiConsumer)
         */
        @Override
        public void onMessage(String channel, String message) {
            onMessageCallbacks.stream().forEach(callback -> callback.accept(channel, message));
        }

        /**
         * Add a callback for +onMessage+.
         *
         * @param callback
         *        The callback to add. The first argument is the
         *        channel, the second is the message.
         * @see org.ulyssis.ipp.utils.JedisHelper.CallBackPubSub#onMessage(String, String)
         */
        public void addOnMessageListener(BiConsumer<String, String> callback) {
            onMessageCallbacks.add(callback);
        }

        /**
         * Implementation of +onPMessage+. Forwards the call to the registered callbacks.
         *
         * @see org.ulyssis.ipp.utils.JedisHelper.CallBackPubSub#addOnPMessageListener(org.ulyssis.ipp.utils.JedisHelper.CallBackPubSub.OnPMessageCallback)
         */
        @Override
        public void onPMessage(String pattern, String channel, String message) {
            onPMessageCallbacks.stream().forEach(callback -> callback.accept(pattern, channel, message));
        }

        /**
         * Add a callback for +onPMessage+.
         *
         * @param callback
         *        The callback to add. The first argument is the
         *        pattern is the channel, the second is the channel,
         *        and the third is the message.
         * @see org.ulyssis.ipp.utils.JedisHelper.CallBackPubSub#onPMessage(String, String, String)
         */
        public void addOnPMessageListener(OnPMessageCallback callback) {
            onPMessageCallbacks.add(callback);
        }

        /**
         * Implementation of +onSubscribe+. Forwards the call to the registered callbacks.
         *
         * @see org.ulyssis.ipp.utils.JedisHelper.CallBackPubSub#addOnSubscribeListener(java.util.function.BiConsumer)
         */
        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            onSubscribeCallbacks.stream().forEach(callback -> callback.accept(channel, subscribedChannels));
        }

        /**
         * Add a callback for +onSubscribe+
         *
         * @param callback
         *        The callback to add. The first argument is the channel,
         *        the second is the number of subscribed channels.
         * @see org.ulyssis.ipp.utils.JedisHelper.CallBackPubSub#onSubscribe(String, int)
         */
        public void addOnSubscribeListener(BiConsumer<String, Integer> callback) {
            onSubscribeCallbacks.add(callback);
        }

        /**
         * Implementation of +onUnsubscribe+. Forwards the call to the registered callbacks.
         *
         * @see org.ulyssis.ipp.utils.JedisHelper.CallBackPubSub#addOnUnsubscribeListener(java.util.function.BiConsumer)
         */
        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {
            onUnsubscribeCallbacks.stream().forEach(callback -> callback.accept(channel, subscribedChannels));
        }

        /**
         * Add a callback for +onUnsubscribe+
         *
         * @param callback
         *        The callback to add. The first argument is the channel,
         *        the second is the number of subscribed channels.
         * @see org.ulyssis.ipp.utils.JedisHelper.CallBackPubSub#onUnsubscribe(String, int)
         */
        public void addOnUnsubscribeListener(BiConsumer<String, Integer> callback) {
            onUnsubscribeCallbacks.add(callback);
        }

        /**
         * Implementation of +onPSubscribe+. Forwards the call to the registered callbacks.
         *
         * @see org.ulyssis.ipp.utils.JedisHelper.CallBackPubSub#addOnPSubscribeListener(java.util.function.BiConsumer)
         */
        @Override
        public void onPSubscribe(String pattern, int subscribedChannels) {
            onPSubscribeCallbacks.stream().forEach(callback -> callback.accept(pattern, subscribedChannels));
        }

        /**
         * Add a callback for +onPSubscribe+
         *
         * @param callback
         *        The callback to add. The first argument is the pattern,
         *        the second is the number of subscribed channels.
         * @see org.ulyssis.ipp.utils.JedisHelper.CallBackPubSub#onPSubscribe(String, int)
         */
        public void addOnPSubscribeListener(BiConsumer<String, Integer> callback) {
            onPSubscribeCallbacks.add(callback);
        }

        /**
         * Implementation of +onPUnsubscribe+. Forwards the call to the registered callbacks.
         *
         * @see org.ulyssis.ipp.utils.JedisHelper.CallBackPubSub#addOnPUnsubscribeListener(java.util.function.BiConsumer)
         */
        @Override
        public void onPUnsubscribe(String pattern, int subscribedChannels) {
            onPUnsubscribeCallbacks.stream().forEach(callback -> callback.accept(pattern, subscribedChannels));
        }

        /**
         * Add a callback for +onPUnsubscribe+
         *
         * @param callback
         *        The callback to add. The first argument is the pattern,
         *        the second is the number of subscribed channels.
         * @see org.ulyssis.ipp.utils.JedisHelper.CallBackPubSub#onPUnsubscribe(String, int)
         */
        public void addOnPUnsubscribeListener(BiConsumer<String, Integer> callback) {
            onPUnsubscribeCallbacks.add(callback);
        }
    }
}
