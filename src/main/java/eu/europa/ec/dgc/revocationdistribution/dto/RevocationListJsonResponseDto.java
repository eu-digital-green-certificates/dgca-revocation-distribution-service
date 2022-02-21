/*-
 * ---license-start
 * eu-digital-green-certificates / dgca-revocation-distribution-service
 * ---
 * Copyright (C) 2022 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package eu.europa.ec.dgc.revocationdistribution.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class RevocationListJsonResponseDto implements Serializable {

    List<RevocationListJsonResponseItemDto> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RevocationListJsonResponseItemDto implements Serializable {

        private String kid;

        private String mode;

        private List<String> hashTypes;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
        private ZonedDateTime expires;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
        private ZonedDateTime lastUpdated;
    }

}
