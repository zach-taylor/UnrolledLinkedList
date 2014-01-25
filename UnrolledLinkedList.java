import java.util.AbstractSequentialList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * 	@author Zach Taylor
 * 
 * 	https://en.wikipedia.org/wiki/Unrolled_linked_list
 * 
 *  Implementation of the list interface based on linked nodes that store
 *  multiple items per node. Rules for adding and removing elements
 *  ensure that each node (except possibly the last one) is at least half
 *  full.
 */
public class UnrolledLinkedList<E extends Comparable<? super E>> extends AbstractSequentialList<E> {
	/**
	 * Default number of elements that may be stored in each node.
	 */
	private static final int DEFAULT_NODESIZE = 4;

	/**
	 * Number of elements that can be stored in each node.
	 */
	private final int nodeSize;

	/**
	 * Dummy node for head.
	 */
	private Node head;

	/**
	 * Dummy node for tail.
	 */
	private Node tail;

	/**
	 * Number of elements in the list.
	 */
	private int size;

	/**
	 * Constructs an empty list with the default node size.
	 */
	public UnrolledLinkedList() {
		this(DEFAULT_NODESIZE);
	}

	/**
	 * Constructs an empty list with the given node size.
	 * 
	 * @param nodeSize
	 *            number of elements that may be stored in each node, must be an
	 *            even number
	 */
	public UnrolledLinkedList(int nodeSize) {
		if (nodeSize <= 0 || nodeSize % 2 != 0)
			throw new IllegalArgumentException();

		// dummy nodes
		head = new Node();
		tail = new Node();
		head.next = tail;
		tail.previous = head;
		this.nodeSize = nodeSize;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean add(E item) {
		add(size, item);
		return true;
	}

	@Override
	public void add(int pos, E item) {
		NodeInfo nodeInfo = find(pos);
		add(nodeInfo.node, nodeInfo.offset, item);
	}

	@Override
	public E remove(int pos) {
		NodeInfo nodeInfo = find(pos);
		Node n = nodeInfo.node;
		int offset = nodeInfo.offset;

		E toReturn = n.data[offset];

		if (n.next == tail && n.count == 1) {
			n.removeItem(offset);
			n.previous.next = tail;
			tail.previous = n.previous;
		} else if (n.next == tail || n.count > nodeSize / 2) {
			n.removeItem(offset);
		} else {
			n.removeItem(offset);
			if (n.next.count > nodeSize / 2) {
				n.addItem(n.next.data[0]);
				n.next.removeItem(0);
			} else {
				for (int i = 0; i < n.next.count; i++) {
					n.addItem(n.next.data[i]);
				}

				// delete n.next
				n.next.next.previous = n;
				n.next = n.next.next;
			}
		}

		size--;
		return toReturn;
	}

	/**
	 * Sort all elements in the unrolled linked list in the NON-DECREASING order. It
	 * does the following: Traverse the list and copy its elements into an array,
	 * deleting every visited node along the way. Then, sort the array by
	 * calling the insertionSort() method. Finally, copy all elements from the array
	 * back to the unrolled linked list, creating new nodes for storage. After sorting,
	 * all nodes but (possibly) the last one must be full of elements.
	 * 
	 * Comparator<E> must have been implemented for calling insertionSort().
	 */
	public void sort() {
		E[] arr = (E[]) new Comparable[size];
		ListIterator<E> iter = this.listIterator();

		int i = 0;
		while (iter.hasNext()) {
			arr[i] = iter.next();
			iter.remove();
			i++;
		}

		Comparator<? super E> comp = new Comparator<E>() {

			@Override
			public int compare(E o1, E o2) {
				return o1.compareTo(o2);
			}

		};

		insertionSort(arr, comp);

		for (int m = 0; m < arr.length; m++) {
			iter.add(arr[m]);
		}
	}

	/**
	 * Sort all elements in the unrolled linked list in the NON-INCREASING order. Call
	 * the bubbleSort() method. After sorting, all but (possibly) the last nodes
	 * must be filled with elements.
	 * 
	 * Comparable<? super E> must be implemented for calling bubbleSort().
	 */
	public void sortReverse() {
		E[] arr = (E[]) new Comparable[size];
		ListIterator<E> iter = this.listIterator();

		int i = 0;
		while (iter.hasNext()) {
			arr[i] = iter.next();
			iter.remove();
			i++;
		}

		bubbleSort(arr);

		for (int m = 0; m < arr.length; m++) {
			iter.add(arr[m]);
		}
	}

	@Override
	public Iterator<E> iterator() {
		return new UnrolledLinkedListIterator();
	}

	@Override
	public ListIterator<E> listIterator() {
		return new UnrolledLinkedListIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return new UnrolledLinkedListIterator(index);
	}

	/**
	 * Finds the Node and offset correlating to the given index. If size ==
	 * index or size == 0, the tail Node and offset 0 are returned.
	 * 
	 * @param index
	 *            the index to find
	 * @return the NodeInfo object containing a Node and offset
	 */
	private NodeInfo find(int index) {
		if (index < 0 || index > size) {
			throw new IndexOutOfBoundsException("" + index);
		}

		if (size == 0) {
			return new NodeInfo(tail, 0);
		}
		if (index == size) {
			return new NodeInfo(tail, 0);
		}

		Node node = head.next;
		int position = 0;
		while (position <= index - node.count) {
			position += node.count;
			node = node.next;
		}

		return new NodeInfo(node, index - position);
	}

	/**
	 * This function returns a NodeInfo object containing the Node and offset
	 * where item was added.
	 * 
	 * The rules for adding an element X at index i are as follows. Remember
	 * that adding an element without specifying an index is the same as adding
	 * at index i = size. For the sake of discussion, assume that the logical
	 * index = size corresponds to node = tail and offset = 0. Suppose that
	 * index i occurs in node n and offset off. (Assume that index = size means
	 * n = tail and off = 0) * if the list is empty, create a new node and put X
	 * at offset 0 * otherwise if off = 0 and one of the following two cases
	 * occurs, o if n has a predecessor which has fewer than M elements (and is
	 * not the head), put X in n’s predecessor� o if n is the tail node and n’s
	 * predecessor has M elements, create a new node and put X at offset 0 *
	 * otherwise if there is space in node n, put X in node n at offset off,
	 * shifting array elements as necessary * otherwise, perform a split
	 * operation: move the last M/2 elements of node n into a new successor node
	 * n' and then o if off <= M/2, put X in node n at offset off o if off >
	 * M/2, put X in node n' at offset (off - M/2)
	 * 
	 * @param n
	 *            the requested node
	 * @param offset
	 *            the requested offset
	 * @param item
	 *            the element to add
	 * @return the NodeInfo object containing where the new element was added
	 */
	private NodeInfo add(Node n, int offset, E item) {
		if (item == null) {
			throw new NullPointerException();
		}

		NodeInfo newNodeInfo = null;

		// conditions for determining actions
		if (size == 0) {
			Node temp = new Node();
			temp.addItem(item);

			temp.previous = head;
			temp.next = tail;
			head.next = temp;
			tail.previous = temp;

			newNodeInfo = new NodeInfo(temp, 0);
		} else if (offset == 0 && n.previous.count < nodeSize
				&& n.previous != head) {
			n.previous.addItem(item);
			newNodeInfo = new NodeInfo(n.previous, n.previous.count - 1);
		} else if (offset == 0 && n == tail && n.previous.count == nodeSize) {
			Node temp = new Node();
			temp.addItem(item);

			temp.next = tail;
			temp.previous = tail.previous;
			tail.previous.next = temp;
			tail.previous = temp;

			newNodeInfo = new NodeInfo(temp, 0);
		} else if (n.count < nodeSize) {
			n.addItem(offset, item);
			newNodeInfo = new NodeInfo(n, offset);
		} else {
			Node temp = new Node();

			temp.previous = n;
			temp.next = n.next;
			n.next.previous = temp;
			n.next = temp;

			int half = nodeSize / 2;
			for (int i = half; i < nodeSize; i++) {
				temp.addItem(n.data[half]);

				// remove n.data[i]
				n.removeItem(half);
			}

			if (offset <= nodeSize / 2) {
				n.addItem(offset, item);
				newNodeInfo = new NodeInfo(n, offset);
			} else {
				temp.addItem(offset - nodeSize / 2, item);
				newNodeInfo = new NodeInfo(temp, offset - nodeSize / 2);
			}
		}

		size++;
		return newNodeInfo;
	}

	/**
	 * Returns a string representation of this list showing the internal
	 * structure of the nodes.
	 */
	public String toStringInternal() {
		return toStringInternal(null);
	}

	/**
	 * Returns a string representation of this list showing the internal
	 * structure of the nodes and the position of the iterator.
	 * 
	 * @param iter
	 *            an iterator for this list
	 */
	public String toStringInternal(ListIterator<E> iter) {
		int count = 0;
		int position = -1;
		if (iter != null) {
			position = iter.nextIndex();
		}

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		Node current = head.next;
		while (current != tail) {
			sb.append('(');
			E data = current.data[0];
			if (data == null) {
				sb.append("-");
			} else {
				if (position == count) {
					sb.append("| ");
					position = -1;
				}
				sb.append(data.toString());
				++count;
			}

			for (int i = 1; i < nodeSize; ++i) {
				sb.append(", ");
				data = current.data[i];
				if (data == null) {
					sb.append("-");
				} else {
					if (position == count) {
						sb.append("| ");
						position = -1;
					}
					sb.append(data.toString());
					++count;

					// iterator at end
					if (position == size && count == size) {
						sb.append(" |");
						position = -1;
					}
				}
			}
			sb.append(')');
			current = current.next;
			if (current != tail)
				sb.append(", ");
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Node type for this list. Each node holds a maximum of nodeSize elements
	 * in an array. Empty slots are null.
	 */
	private class Node {
		/**
		 * Array of actual data elements.
		 */
		// Unchecked warning unavoidable.
		public E[] data = (E[]) new Comparable[nodeSize];

		/**
		 * Link to next node.
		 */
		public Node next;

		/**
		 * Link to previous node;
		 */
		public Node previous;

		/**
		 * Index of the next available offset in this node, also equal to the
		 * number of elements in this node.
		 */
		public int count;

		/**
		 * Adds an item to this node at the first available offset.
		 * Precondition: count < nodeSize
		 * 
		 * @param item
		 *            element to be added
		 */
		void addItem(E item) {
			if (count >= nodeSize) {
				return;
			}
			data[count++] = item;
			// useful for debugging
			// System.out.println("Added " + item.toString() + " at index " +
			// count + " to node " + Arrays.toString(data));
		}

		/**
		 * Adds an item to this node at the indicated offset, shifting elements
		 * to the right as necessary.
		 * 
		 * Precondition: count < nodeSize
		 * 
		 * @param offset
		 *            array index at which to put the new element
		 * @param item
		 *            element to be added
		 */
		void addItem(int offset, E item) {
			if (count >= nodeSize) {
				return;
			}
			for (int i = count - 1; i >= offset; --i) {
				data[i + 1] = data[i];
			}
			++count;
			data[offset] = item;
			// useful for debugging
			// System.out.println("Added " + item.toString() + " at index " +
			// offset + " to node: " + Arrays.toString(data));
		}

		/**
		 * Deletes an element from this node at the indicated offset, shifting
		 * elements left as necessary. Precondition: 0 <= offset < count
		 * 
		 * @param offset
		 */
		void removeItem(int offset) {
			E item = data[offset];
			for (int i = offset + 1; i < nodeSize; ++i) {
				data[i - 1] = data[i];
			}
			data[count - 1] = null;
			--count;
		}
	}

	/**
	 * Helper class used to store a Node and offset.
	 */
	private class NodeInfo {
		public Node node;
		public int offset;

		/**
		 * Constructor
		 * 
		 * @param node
		 *            the node to be stored
		 * @param offset
		 *            the offset to be stored
		 */
		public NodeInfo(Node node, int offset) {
			this.node = node;
			this.offset = offset;
		}
	}

	/**
	 * Iterator for the UnrolledLinkedList
	 */
	private class UnrolledLinkedListIterator implements ListIterator<E> {
		// constants you possibly use ...
		private static final int BEHIND = -1;
		private static final int AHEAD = 1;
		private static final int NONE = 0;

		// instance variables ...
		private Node curNode;
		private E lastItem;
		private int index;
		private int cursor;
		private int direction;

		/**
		 * Default constructor
		 */
		public UnrolledLinkedListIterator() {
			this(0);
		}

		/**
		 * Constructor finds node at a given position.
		 * 
		 * @param pos
		 *            the given position
		 */
		public UnrolledLinkedListIterator(int pos) {
			NodeInfo nodeInfo = find(pos);

			cursor = nodeInfo.offset;
			curNode = nodeInfo.node;
			index = pos;
			direction = NONE;
		}

		@Override
		public boolean hasNext() {
			return index < size;
		}

		@Override
		public E next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			lastItem = curNode.data[cursor];
			cursor++;
			if (cursor >= curNode.count) {
				curNode = curNode.next;
				cursor = 0;
			}
			index++;
			direction = BEHIND;
			return lastItem;
		}

		@Override
		public void remove() {
			if (direction == NONE) {
				throw new IllegalStateException();
			} else if (direction == AHEAD) { // delete element in front of
												// cursor
			} else { // delete element behind cursor
				cursor--;
				if (cursor < 0) {
					curNode = curNode.previous;
					cursor = curNode.count - 1;
				}
				index--;
			}

			Node n = curNode;
			int offset = cursor;

			// conditions for how to remove
			if (n.next == tail && n.count == 1) {
				n.removeItem(offset);
				n.previous.next = tail;
				tail.previous = n.previous;
			} else if (n.next == tail || n.count > nodeSize / 2) {
				n.removeItem(offset);
			} else {
				n.removeItem(offset);
				if (n.next.count > nodeSize / 2) {
					n.addItem(n.next.data[0]);
					n.next.removeItem(0);
				} else {
					for (int i = 0; i < n.next.count; i++) {
						n.addItem(n.next.data[i]);
					}

					// delete n.next
					n.next.next.previous = n;
					n.next = n.next.next;
				}
			}

			--size;
			direction = NONE;
		}

		@Override
		public void add(E item) {
			NodeInfo nodeInfo = UnrolledLinkedList.this.add(curNode, cursor, item);
			cursor = nodeInfo.offset + 1;
			if (cursor >= nodeSize) {
				curNode = nodeInfo.node.next;
				cursor = 0;
			} else {
				curNode = nodeInfo.node;
			}
			index++;
			direction = NONE;
		}

		@Override
		public boolean hasPrevious() {
			return index > 0;
		}

		@Override
		public int nextIndex() {
			return index;
		}

		@Override
		public E previous() {
			if (!hasPrevious()) {
				throw new NoSuchElementException();
			}

			cursor--;
			if (cursor < 0) {
				curNode = curNode.previous;
				cursor = curNode.count - 1;
			}

			index--;
			direction = AHEAD;
			lastItem = curNode.data[cursor];
			return lastItem;
		}

		@Override
		public int previousIndex() {
			return index - 1;
		}

		@Override
		public void set(E item) {
			if (direction == NONE) {
				throw new IllegalStateException();
			} else if (direction == AHEAD) {
				curNode.data[cursor] = item;
			} else {
				if (cursor > 0) {
					curNode.data[cursor - 1] = item;
				} else {
					curNode.previous.data[curNode.previous.count - 1] = item;
				}
			}
		}
	}

	/**
	 * Sort an array arr[] using the insertion sort algorithm in the
	 * NON-DECREASING order.
	 * 
	 * @param arr
	 *            array storing elements from the list
	 * @param comp
	 *            comparator used in sorting
	 */
	private void insertionSort(E[] arr, Comparator<? super E> comp) {
		for (int i = 1; i < arr.length; i++) {
			E t = arr[i];
			int j;
			//condition: j >= 0 and arr[j] > t
			for (j = i - 1; j >= 0 && comp.compare(arr[j], t) >= 0; j--) {
				arr[j + 1] = arr[j];
			}
			arr[j + 1] = t;
		}
	}

	/**
	 * Sort arr[] using the bubble sort algorithm in the NON-INCREASING order.
	 * 
	 * @param arr
	 *            array holding elements from the list
	 */
	private void bubbleSort(E[] arr) {
		for (int i = 0; i < arr.length; i++) {
			for (int j = 1; j < arr.length - i; j++) {
				if (arr[j - 1].compareTo(arr[j]) < 0) {
					//swap
					E t = arr[j - 1];
					arr[j - 1] = arr[j];
					arr[j] = t;
				}
			}
		}
	}

}