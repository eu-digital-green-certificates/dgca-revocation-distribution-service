/*-
 * ---license-start
 * EU Digital Green Certificate Gateway Service / dgc-gateway
 * ---
 * Copyright (C) 2021 T-Systems International GmbH and all other contributors
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

package europa.ec.dgc.revocationdistribution.config;

import europa.ec.dgc.revocationdistribution.exception.DataNotFoundException;
import europa.ec.dgc.revocationdistribution.exception.PreconditionFaildException;
import javax.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ErrorHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles {@link PreconditionFaildException} when a validation failed.
     *
     * @param e the thrown {@link PreconditionFaildException}
     * @return A ResponseEntity with a ErrorMessage inside.
     */
    @ExceptionHandler(PreconditionFaildException.class)
    public ResponseEntity<Object> handleException(PreconditionFaildException e) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();

    }

    /**
     * Handles {@link DataNotFoundException} when a validation failed.
     *
     * @param e the thrown {@link DataNotFoundException}
     * @return A ResponseEntity with a ErrorMessage inside.
     */
    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<Object> handleException(DataNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

    }


    /**
     * Global Exception Handler to wrap exceptions into a readable JSON Object.
     *
     * @param e the thrown exception
     * @return ResponseEntity with readable data.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body("");
    }
}
