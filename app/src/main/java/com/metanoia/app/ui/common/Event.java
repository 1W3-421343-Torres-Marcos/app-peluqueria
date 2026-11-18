package com.metanoia.app.ui.common;

/**
 * Wrapper para eventos de un solo uso expuestos a través de LiveData.
 *
 * @param <T> tipo del contenido del evento.
 */
public final class Event<T> {

    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    public T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    public T peekContent() {
        return content;
    }

    public boolean hasBeenHandled() {
        return handled;
    }
}
