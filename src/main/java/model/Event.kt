package model

import io.swagger.v3.oas.annotations.media.Schema
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import java.math.BigDecimal

@Data
@AllArgsConstructor
@NoArgsConstructor
class Event {
    @Schema(required = true, description = "Custom event type")
    private val type: String? = null

    @Schema(description = "Custom event target / subject of event. Contextual to event type")
    private val target: String? = null

    @Schema(description = "Unix epoch time the event occurred according to client")
    private val date: Long? = null

    @Schema(description = "Value for numerical events. Contextual to event type")
    private val value: BigDecimal? = null

    @Schema(description = "Extra JSON metadata for event. Contextual to event type")
    private val metaData: Any? = null
}