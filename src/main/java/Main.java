import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spaziocodice.labs.solr.qty.cfg.Unit;

import java.io.File;
import java.util.List;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public class Main {
    public static void main(String args[]) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode configuration = mapper.readTree(new File("/Users/agazzarini/IdeaProjects/solr-quantities-detection-qparsers/src/test/resources/solr.home/test/conf/units.json"));
        final List<Unit> units =  stream(configuration.get("units").spliterator(), false)
            .map(unitNode -> {
                final String fieldName =  unitNode.fieldNames().next();
                final JsonNode unitCfg = unitNode.get(fieldName);

                final String unitName = unitCfg.get("unit").asText();

                final Unit unit = new Unit(
                        fieldName,
                        unitName, unitCfg.hasNonNull("boost") ? unitCfg.get("boost").floatValue() : 1f);

                ofNullable(unitCfg.get("gap"))
                        .ifPresent(gap -> {
                            unit.setGap(
                                    gap.get("value").floatValue(),
                                    gap.get("mode").asText("PIVOT"));
                        });

                ofNullable(unitCfg.get("variants"))
                        .ifPresent(variants -> {
                            variants.fieldNames().forEachRemaining(mainFormName -> {
                                unit.addVariant(
                                        mainFormName,
                                        stream(variants.get(mainFormName).spliterator(), false)
                                                .map(JsonNode::asText)
                                                .collect(toList()));
                            });
                        });
                return unit;
            }).collect(toList());
        System.out.println();
    }
}
