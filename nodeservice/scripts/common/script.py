import os

def delete_files_and_lines(target_file_string, target_line_string, target_function_start, target_function_end):
    # Get the directory where the script is located
    base_directory = os.getcwd()
    script_name = os.path.basename(__file__)

    # Traverse through all directories and files
    for root, dirs, files in os.walk(base_directory):
        for file_name in files:
            # Skip the script itself
            if file_name == script_name:
                continue

            file_path = os.path.join(root, file_name)
            try:
                # Open the file and check its contents
                with open(file_path, 'r', encoding='utf-8', errors='ignore') as file:
                    lines = file.readlines()

                # Check if the target file string is in the file
                if any(target_file_string in line for line in lines):
                    print(f"Deleting file: {file_path}")
                    os.remove(file_path)  # Delete the file
                    continue

                # Remove lines containing the target line string
                new_lines = []
                inside_function_block = False

                for line in lines:
                    if target_function_start in line:
                        inside_function_block = True
                        print(f"Removing block starting with '{target_function_start}' in file: {file_path}")
                        continue

                    if target_function_end in line:
                        inside_function_block = False
                        print(f"Removing block ending with '{target_function_end}' in file: {file_path}")
                        continue

                    if inside_function_block or target_line_string in line:
                        if target_line_string in line:
                            print(f"Removing line containing '{target_line_string}' in file: {file_path}")
                        continue

                    new_lines.append(line)

                # Rewrite the file if modifications were made
                if len(new_lines) < len(lines):
                    with open(file_path, 'w', encoding='utf-8') as file:
                        file.writelines(new_lines)

            except Exception as e:
                print(f"Could not process file {file_path}. Error: {e}")

if __name__ == "__main__":
    # Define the services to check for and their corresponding strings
    services = {
        "emailservice-main": {
            "file": "Integration file: Email",
            "line": "Integration line: Email",
            "function_start": "Integration function start: Email",
            "function_end": "Integration function end: Email"
        },
        "authservice-main": {
            "file": "Integration file: Auth",
            "line": "Integration line: Auth",
            "function_start": "Integration function start: Auth",
            "function_end": "Integration function end: Auth"
        },
        "reactservice-main": {
            "file": "Integration file: React",
            "line": "Integration line: React",
            "function_start": "Integration function start: React",
            "function_end": "Integration function end: React"
        },
        "testservice-main": {
            "file": "Integration file: Test",
            "line": "Integration line: Test",
            "function_start": "Integration function start: Test",
            "function_end": "Integration function end: Test"
        },
        "gatewayservice-main": {
            "file": "Integration file: Gateway",
            "line": "Integration line: Gateway",
            "function_start": "Integration function start: Gateway",
            "function_end": "Integration function end: Gateway"
        },
        "vaultservice-main": {
            "file": "Integration file: Vault",
            "line": "Integration line: Vault",
            "function_start": "Integration function start: Vault",
            "function_end": "Integration function end: Vault"
        }
    }

    # Check if any of the service folders exist
    for service, strings in services.items():
        if not os.path.exists(os.path.join(os.getcwd(), service)):
            print(f"Service folder '{service}' not found. Running cleanup for this service.")
            delete_files_and_lines(strings["file"], strings["line"], strings["function_start"], strings["function_end"])
        else:
            print(f"Service folder '{service}' exists. Skipping cleanup for this service.")
