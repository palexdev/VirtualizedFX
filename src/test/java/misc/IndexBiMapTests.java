package misc;

import io.github.palexdev.virtualizedfx.utils.IndexBiMap;
import model.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class IndexBiMapTests {

	@Test
	void testDuplicates1() {
		List<String> strings = List.of(
			"String 0",
			"String 1",
			"String 2",
			"String 3",
			"String 4",
			"String 5",
			"String 6",
			"String 0",
			"String 3",
			"String 0"
		);
		IndexBiMap<String, Integer> map = new IndexBiMap<>();
		for (int i = 0; i < strings.size(); i++) {
			String s = strings.get(i);
			map.put(i, s, Integer.valueOf(s.split(" ")[1]));
		}
		assertTrue(map.isValid());
	}

	@Test
	void testDuplicates2() {
		List<User> users = List.of(
			new User("A", "A", 0),
			new User("B", "B", 1),
			new User("A", "A", 0),
			new User("B", "B", 1),
			new User("C", "C", 2),
			new User("B", "B", 9)
		);
		IndexBiMap<User, Integer> map = new IndexBiMap<>();
		for (int i = 0; i < users.size(); i++) {
			User user = users.get(i);
			map.put(i, user, user.birthYear());
		}
		assertTrue(map.isValid());
	}

	@Test
	void testDuplicates3() {
		User uA = new User("A", "A", 0);
		User uB = new User("B", "B", 1);
		User uC = new User("C", "C", 2);
		User uB9 = new User("B", "B", 9);
		List<User> users = List.of(uA, uB, uA, uB, uC, uB9);
		IndexBiMap<User, Integer> map = new IndexBiMap<>();
		for (int i = 0; i < users.size(); i++) {
			User user = users.get(i);
			map.put(i, user, user.birthYear());
		}
		assertTrue(map.isValid());
	}
}
