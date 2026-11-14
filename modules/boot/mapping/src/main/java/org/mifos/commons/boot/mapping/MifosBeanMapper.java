///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.mapping;

import java.util.Map;
import org.mapstruct.Mapper;
import tools.jackson.databind.ObjectMapper;

@Mapper(config = MifosMapperConfiguration.class)
public interface MifosBeanMapper {
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    default Map<String, String> map(Object source) {
        var typeFactory = OBJECT_MAPPER.getTypeFactory();
        return OBJECT_MAPPER.convertValue(source, typeFactory.constructMapType(Map.class, String.class, String.class));
    }
}
