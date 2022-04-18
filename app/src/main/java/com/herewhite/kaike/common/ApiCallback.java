package com.herewhite.kaike.common;

public interface ApiCallback<T> {
    void onSuccess(T data);

    void onFailure(String message);
}