import subprocess

networks = [
    'shared-network',
    'authdb-network',
    'emaildb-network'
]

networkString = subprocess.run(['docker', 'network', 'ls'], capture_output=True, text=True).stdout.strip("\n")

for network in networks:
    if network not in networkString:
        output = subprocess.run(['docker', 'network', 'create', network], capture_output=True, text=True).stdout.strip("\n")
        print("Created " + network + ": " + output)
