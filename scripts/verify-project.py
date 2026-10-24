#!/usr/bin/env python3
"""Dependency-free static source checks. Not a substitute for Maven tests or terraform validate."""
from pathlib import Path
from xml.etree import ElementTree as ET
import json,ast,subprocess,sys
root=Path(__file__).resolve().parents[1]
needed=['pom.xml','compose.yaml','Dockerfile','.env.example',
 'src/main/resources/db/migration/V1__schema.sql',
 'src/main/java/dev/portfolio/bankflow/service/WorkflowEngine.java',
 'src/main/java/dev/portfolio/bankflow/service/CaseService.java',
 'src/test/java/dev/portfolio/bankflow/ModelEvaluationTest.java',
 'infra/terraform/main.tf','infra/terraform/variables.tf',
 'src/main/resources/static/index.html']
for entry in needed:
    assert (root/entry).is_file(),f'Missing: {entry}'
ET.parse(root/'pom.xml')
json.loads((root/'src/main/resources/policies.json').read_text())
json.loads((root/'src/test/resources/evals/banking-cases.json').read_text())
ast.parse((root/'mock-core/server.py').read_text())
ast.parse((root/'scripts/evaluate-live.py').read_text())
for file in root.rglob('*.sh'):
    subprocess.run(['bash','-n',str(file)],check=True)
assert 'CREATE EXTENSION IF NOT EXISTS vector' in (root/'src/main/resources/db/migration/V1__schema.sql').read_text()
print('PASS static required file, XML, JSON, Python and shell validation')
