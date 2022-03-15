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

package eu.europa.ec.dgc.revocationdistribution.config;

import eu.europa.ec.dgc.revocationdistribution.model.SliceType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("dgc")
public class DgcConfigProperties {

    private final GatewayDownload revocationListDownload = new GatewayDownload();
    private final BloomFilterConfig bloomFilter = new BloomFilterConfig();
    private final VarHashListConfig varHashList = new VarHashListConfig();

    private final SliceType defaultRevocationDataType = SliceType.BLOOMFILTER;//"BLOOMFILTER";


    @Getter
    @Setter
    public static class GatewayDownload {
        private Integer timeInterval;
        private Integer lockLimit;
    }

    @Getter
    @Setter
    public static class BloomFilterConfig {
        private boolean enabled;
        private String type = "bloom_filter";
        private String version;
        private float probRate;
    }

    @Getter
    @Setter
    public static class VarHashListConfig {
        private boolean enabled;
        private String type = "hash_list";
        private String version;
        private float probRate;
        private byte minByteCount = 4;

    }
}
