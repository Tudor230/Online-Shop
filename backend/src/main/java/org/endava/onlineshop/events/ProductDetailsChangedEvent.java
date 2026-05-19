package org.endava.onlineshop.events;

import java.util.UUID;

public record ProductDetailsChangedEvent(UUID productId) {
}
