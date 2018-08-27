package booot.config;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * 0_0
 * 
 * @author kakshonau
 *
 */
class ExceptionNodeAccessor {
    private final Node root = new Node();

    public void add(Class<? extends Throwable> throwable) {
        Deque<Class<? extends Throwable>> deque = new LinkedList<>();
        Class<? extends Throwable> clazz = throwable;
        while (!Object.class.equals(clazz)) {
            deque.add(clazz);
            clazz = (Class<? extends Throwable>) clazz.getSuperclass();
        }
        Class<? extends Throwable> pollLast = deque.pollLast();
        pollLast = deque.pollLast();
        Node currentNode = root;
        while (null != pollLast) {
            boolean exists = false;
            for (Node node : currentNode.nodes) {
                if (node.clazz == pollLast) {
                    exists = true;
                    currentNode = node;
                    break;
                }
            }
            if (!exists) {
                Node newNode = new Node();
                newNode.clazz = pollLast;
                currentNode.nodes.add(newNode);
                currentNode = newNode;
            }
            pollLast = deque.pollLast();
        }
        currentNode.synthetic = false;
    }

    public Class<? extends Throwable> findNearest(Throwable t) {
        Deque<Class<? extends Throwable>> deque = new LinkedList<>();
        Class<? extends Throwable> clazz = t.getClass();
        while (!Object.class.equals(clazz)) {
            deque.add(clazz);
            clazz = (Class<? extends Throwable>) clazz.getSuperclass();
        }
        Class<? extends Throwable> nearest = null;
        Class<? extends Throwable> pollLast = deque.pollLast();
        Node currentNode = root;
        while (null != pollLast && currentNode.clazz == pollLast) {
            if (!currentNode.synthetic) {
                nearest = currentNode.clazz;
            }
            pollLast = deque.pollLast();
            boolean exists = false;
            for (Node node : currentNode.nodes) {
                if (node.clazz == pollLast) {
                    exists = true;
                    currentNode = node;
                    break;
                }
            }
            if (!exists) {
                pollLast = null;
            }
        }
        return nearest;
    }

    private static final class Node {
        private final Set<Node> nodes = new HashSet<>();
        private boolean synthetic = true;
        private Class<? extends Throwable> clazz = Throwable.class;
    }
}
