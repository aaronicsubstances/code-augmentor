import { assert } from "chai";

import CodeChangeDetective from "../src/CodeChangeDetective";

describe("CodeChangeDetective", function() {
    describe("#execute", function() {
        it("should pass", async function() {
            const instance = new CodeChangeDetective();
            instance.codeChangeSupplier = async function() {
                return null;
            };
            instance.codeChangeProcessingErrorLog = null;
            instance.destDir = '';
            instance.cleanDestDir = true;
            instance.codeChangeDetectionDisabled = false;
            instance.defaultEncoding = null;
            instance.outputSummaryPath = '';
            instance.changeSummaryPath = '';
            instance.changeDetailsPath = '';
            const expected = false;
            const actual = await instance.execute();
            assert.equal(actual, expected);
        });
    });
});
