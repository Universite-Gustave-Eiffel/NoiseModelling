import logging
import sys
from owslib.wps import WebProcessingService, monitorExecution

def main():
    # Configure the root logger to output to stdout
    logging.basicConfig(stream=sys.stdout, level=logging.INFO)

    url = "http://localhost:8000/builder/ows"

    # use java web token for authentification
    jwt_token = ""
    wps = WebProcessingService(url, skip_caps=False, headers={"Authorization": jwt_token})

    target_id = "Database_Manager:Display_Database"

    inputs = [('showColumns', "true")]

    # Execute (sync or async)
    execution = wps.execute(target_id, inputs)

    # Monitor progress if it's a long task
    monitorExecution(execution, sleepSecs=2)

    if execution.isSucceded():
        for output in execution.processOutputs:
            print(f"Result {output.identifier}: {output.data}")
    else:
        print("Execution failed.")

if __name__ == "__main__":
    main()