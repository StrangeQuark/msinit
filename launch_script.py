import subprocess
import os
import platform

networks = [
    'shared-network',
    'authdb-network',
    'emaildb-network'
]

networkString = subprocess.run(['docker', 'network', 'ls'], capture_output=True, text=True).stdout.strip("\n")

# Create all the missing networks
for network in networks:
    if network not in networkString:
        output = subprocess.run(['docker', 'network', 'create', network], capture_output=True, text=True).stdout.strip("\n")
        print("Created " + network + ": " + output)

# Iterate through each service and run docker-compose up --build in a new terminal window
base_dir = os.getcwd()  # Get the current working directory

for folder in os.listdir(base_dir):
    folder_path = os.path.join(base_dir, folder)
    
    if os.path.isdir(folder_path) and 'docker-compose.yml' in os.listdir(folder_path):
        try:
            system = platform.system()
            command = ['docker-compose', 'up', '--build']

            if system == "Windows":
                subprocess.Popen(['start', 'cmd', '/k'] + command, cwd=folder_path, shell=True)
            elif system == "Darwin":  # macOS
                subprocess.Popen(['osascript', '-e', 
                    f'tell application \"Terminal\" to do script \"cd {folder_path} && {" ".join(command)}\"'])
            elif system == "Linux":
                subprocess.Popen(['x-terminal-emulator', '-e'] + command, cwd=folder_path)
            else:
                print(f"Unsupported OS: {system}")
        except Exception as e:
            print(f"Error running docker-compose in {folder_path}: {e}")
