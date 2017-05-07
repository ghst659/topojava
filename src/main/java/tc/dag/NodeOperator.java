package tc.dag;

interface NodeOperator<T, V> {
    V operate(T node) throws Exception;
}
