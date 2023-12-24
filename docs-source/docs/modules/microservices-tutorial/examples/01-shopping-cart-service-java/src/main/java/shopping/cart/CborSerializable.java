package shopping.cart;

/**
 * Marker trait for serialization with Jackson CBOR. Enabled in serialization.conf
 * `pekko.actor.serialization-bindings` (via application.conf).
 */
public interface CborSerializable {}
