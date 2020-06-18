package nsdw.vertiv.utils

import java.util.function.Consumer

class ConciseCodeUtil {

    static <T> void onNotNull(T value, Consumer<T> consumer) {
        if(value!=null) {
            consumer.accept value
        }
    }

}
