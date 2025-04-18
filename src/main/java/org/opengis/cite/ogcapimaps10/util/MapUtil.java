package org.opengis.cite.ogcapimaps10.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapUtil {

	public static List<URL> fetchMapUrls(URI rootUri) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		String apiUrl = rootUri.toString() + "/collections?f=json";

		Map<String, Object> data = objectMapper.readValue(new URL(apiUrl), Map.class);
		List<Map<String, Object>> collectionsList = (List<Map<String, Object>>) data.get("collections");
		List<URL> urls = new ArrayList<>();

		for (Map<String, Object> collection : collectionsList) {
			List<Map<String, Object>> collectionLinks = (List<Map<String, Object>>) collection.get("links");
			Map<String, Object> relMap = findLinkByRel(collectionLinks, "http://www.opengis.net/def/rel/ogc/1.0/map");
			if (relMap != null) {
				String mapUrl = (String) relMap.get("href");
				URI uri = new URI(mapUrl);
				if (!uri.isAbsolute()) {
					uri = rootUri.resolve(uri);
				}
				urls.add(uri.toURL());
			}
		}
		return urls;
	}

	public static Map<String, Object> findLinkByRel(List<Map<String, Object>> links, String expectedRel) {
		if (links == null) {
			return null;
		}
		for (Map<String, Object> link : links) {
			Object rel = link.get("rel");
			if (expectedRel.equals(rel)) {
				return link;
			}
		}
		return null;
	}

}