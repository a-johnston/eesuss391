package edu.cwru.sepia.agent.planner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Presents the Cartesian product of a list of lists in an iterable and
 * streamable style.
 * 
 * For example, if the constructor is called with the list ((1, 2), (3, 4)) the
 * values iterated over will be:
 * 
 * (1, 3)
 * (1, 4)
 * (2, 3)
 * (2, 4)
 * 
 * Handles null or empty lists.
 * 
 * @author adam
 *
 * @param <T>
 */
public class CartesianProduct<T> implements Iterator<List<T>> {

	private List<T> values;
	private List<List<T>> subview;
	private CartesianProduct<T> subproduct;

	private CartesianProduct(List<List<T>> values) {
		if (values == null || values.size() == 0) {
			this.values = Collections.emptyList();
			return;
		}

		this.values = new ArrayList<T>(values.get(0));

		if (values.size() > 1) {
			subview	   = values.subList(1, values.size());
			subproduct = new CartesianProduct<>(subview);
		}
	}

	@Override
	public boolean hasNext() {
		// the current list of values can only be iterated over again if the
		// subproduct an be iterated over again. ugly edge case
		return values.size() > 1 || (values.size() == 1 && (subproduct == null || subproduct.hasNext()));
	}

	@Override
	public List<T> next() {
		List<T> returnList = new ArrayList<T>();
		if(values.size() == 0) {
			return returnList;
		}
		returnList.add(values.get(0));
		if (subproduct == null) {
			// if there's no subproduct, just iterate over this list
			values.remove(0);
		} else if (subproduct.hasNext()) {
			// iterate over subproduct
			returnList.addAll(subproduct.next());
		} else {
			// iterate over this list and reset the subproduct
			values.remove(0);
			subproduct = new CartesianProduct<>(subview);
			return next();
		}
		return returnList;
	}

	/**
	 * Returns an iterable view of the Cartesian product for use in enhanced
	 * for loops.
	 * 
	 * @param values
	 * @return
	 */
	public static <S> Iterable<List<S>> iterate(List<List<S>> values) {
		return new Iterable<List<S>>() {
			@Override
			public Iterator<List<S>> iterator() {
				return new CartesianProduct<S>(values);
			}
		};
	}

	/**
	 * Returns a stream view of the Cartesian product for use in the Streams API
	 * 
	 * @param values
	 * @return
	 */
	public static <S> Stream<List<S>> stream(List<List<S>> values) {
		return StreamSupport.stream(iterate(values).spliterator(), false);
	}
}
