import logging
import sys
from owslib.wps import WebProcessingService

def main():
    # Configure the root logger to output to stdout
    logging.basicConfig(stream=sys.stdout, level=logging.INFO)

    url = "http://localhost:8000/builder/ows"

    # use jwt cookie header for authentification
    jwt_token = ""
    wps = WebProcessingService(url, skip_caps=False, headers={"Authorization": jwt_token})

    # 2. List all available processes
    print(f"\n--- Available Processes ---")
    for process in wps.processes:
        print(f"ID: {process.identifier}")
        print(f"   Title: {process.title}")
        print(f"   Abstract: {process.abstract}\n")
        process_details = wps.describeprocess(process.identifier)
        print(f"   Inputs:")
        for data_input in process_details.dataInputs:
            print(f"      ID: {data_input.identifier}")
            if hasattr(data_input, 'title'):
                print(f"      Title: {data_input.title}")
            if hasattr(data_input, 'dataType'):
                print(f"      Data Type: {data_input.dataType}")
            if hasattr(data_input, 'minOccurs') and data_input.minOccurs == 0:
                print(f"      Optional")
            if hasattr(data_input, 'abstract'):
                print(f"      Abstract: {data_input.abstract}")
            if hasattr(data_input, 'allowedValues') and len(data_input.allowedValues) > 0:
                print(f"      Allowed values: {data_input.allowedValues}")
            if hasattr(data_input, 'defaultValue') and data_input.defaultValue is not None:
                print(f"      Default : {data_input.defaultValue}")
            print("")

if __name__ == "__main__":
    main()
