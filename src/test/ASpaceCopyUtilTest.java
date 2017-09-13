package test;

import org.archiviststoolkit.plugin.utils.aspace.ASpaceClient;
import org.archiviststoolkit.plugin.utils.aspace.ASpaceCopyUtil;
import org.junit.Test;

public class ASpaceCopyUtilTest {

    @Test
    public void saveRecord(){
        ASpaceCopyUtil tester = TestUtils.copyUtil;
        String json = "{\"source\":\"tgn\",\"vocabulary\":\"/vocabularies/1\",\"external_ids\":[{\"external_id\":\"1\","
            + "\"source\":\"Archivists Toolkit Database::SUBJECT\"}],\"terms\":[{\"vocabulary\":\"/vocabularies/1\",\""
            + "term\":\"A test subject\",\"term_type\":\"function\"}],\"publish\":true}";
        tester.saveRecord(ASpaceClient.SUBJECT_ENDPOINT, json, "subject term here");
    }
}
