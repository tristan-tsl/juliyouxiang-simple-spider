package com.jlyx.middleenddataspider.model;

public class CustomException extends Exception {
    public static CustomException build() {
        return new CustomException();
    }
}
