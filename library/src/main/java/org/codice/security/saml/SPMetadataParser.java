/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.security.saml;

import static java.util.Objects.nonNull;

import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.codice.security.saml.SamlProtocol.Binding;

/** Non-instantiable class that provides a utility function to parse service provider metadata */
public class SPMetadataParser {

  private SPMetadataParser() {}

  /**
   * @param spMetadata Metadata from the service provider either as the xml itself, a url to a
   *     service that returns the xml, or the path to a file with the xml starting with file:
   * @param bindingSet Set of supported bindings
   * @return Map of the service providers entity id and the entity information
   */
  public static Map<String, EntityInformation> parse(
      @Nullable String spMetadata, Set<Binding> bindingSet) {
    if (spMetadata == null) {
      return Collections.emptyMap();
    }

    Map<String, EntityInformation> spMap = new HashMap<>();
    MetadataConfigurationParser metadataConfigurationParser =
        new MetadataConfigurationParser(
            spMetadata,
            ed -> {
              EntityInformation entityInfo = new EntityInformation.Builder(ed, bindingSet).build();
              if (entityInfo != null) {
                spMap.put(ed.getEntityID(), entityInfo);
              }
            });

    spMap.putAll(
        metadataConfigurationParser
            .getEntryDescriptions()
            .entrySet()
            .stream()
            .map(
                e ->
                    Maps.immutableEntry(
                        e.getKey(),
                        new EntityInformation.Builder(e.getValue(), bindingSet).build()))
            .filter(e -> nonNull(e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    return spMap;
  }
}
