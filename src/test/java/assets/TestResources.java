package assets;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

public class TestResources {

	//================================================================================
	// Constructors
	//================================================================================
	private TestResources() {}

	//================================================================================
	// Methods
	//================================================================================
	public static <T> T gsonLoad(String res, Type type) {
		Gson gson = new Gson();
		InputStream resStream = TestResources.class.getResourceAsStream(res);
		return gson.fromJson(new InputStreamReader(resStream), type);
	}

	public static <T> String toJson(T obj) {
		Gson gson = new Gson();
		return gson.toJson(obj);
	}
}
