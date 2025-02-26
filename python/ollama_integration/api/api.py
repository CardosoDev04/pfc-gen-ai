from flask import Flask, jsonify, request
import sys
import os

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from services.prompter import get_results
from services.automator import generate_script
import os, sys


app = Flask(__name__)

@app.route('/interactions', methods=['GET'])
def interactions():
    query_params = request.args
    if 'url' in query_params:
        return jsonify({"results": get_results(query_params['url'])})
    return jsonify({"error": "URL was not provided"}), 400    

@app.route('/action-script', methods=['GET'])
def action_script():
    request_data = request.get_json()
    
    if not request_data:
        return jsonify({"error": "No data provided"}), 400
    
    if 'goal' not in request_data or 'interactions' not in request_data:
        return jsonify({"error": "Malformed request, you must include goal and interactions"}), 400
    
    goal = request_data['goal']
    interactions = request_data['interactions']
    
    results = generate_script(goal, interactions)  
    
    return jsonify({"script" : results})
    
if __name__ == '__main__':
    CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
    sys.path.append(os.path.dirname(CURRENT_DIR))
    app.run(debug=True, port=5001)