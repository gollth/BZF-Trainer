#!/usr/bin/env/python3

import json
import random
import os
import os.path as p
from xml.dom.minidom import parseString
import xml.etree.ElementTree as ET
from argparse import ArgumentParser


def convert(file, prefix, shuffle=True):
    with open(file) as f:
        catalog = json.load(f)

        xmlq = ET.Element('resources')
        xmla = ET.Element('resources')
        xmls = ET.Element('resources')
        questions = ET.SubElement(xmlq, 'string-array')
        questions.set('name', '{}_questions'.format(prefix))
        answers = ET.SubElement(xmla, 'string-array')
        answers.set('name', '{}_answers'.format(prefix))
        answers.set('formatted', 'false')
        solutions = ET.SubElement(xmls, 'integer-array')
        solutions.set('name', '{}_solutions'.format(prefix))

        for item in catalog:
            # Error Checking
            for key in ['answers', 'question']:
                if key not in item:
                    raise ValueError('No "{}" field found in JSON file'.format(key))

            if len(item['answers']) != 4:
                raise ValueError('Question {} has not 4 but {} answers.'.format(item['question']['number'], len(item['answers'])))


            if shuffle: random.shuffle(item['answers'])
            a = []
            for letter, answer in zip('A B C D'.split(), item['answers']):
                a.append('{}) {}'.format(letter, answer['text']))
                if answer['correct'] == 'yes':
                    solution = ET.Element('item')
                    solution.text = str(ord(letter) - ord('A'))
                    solutions.append(solution)

            sep = ';' + os.linesep
            answer = ET.Element('item')
            answer.text = sep.join(a)
            answers.append(answer)

            question = ET.Element('item')
            question.text = '{}. {}'.format(item['question']['number'], item['question']['text'])
            questions.append(question)

        return (
            parseString(ET.tostring(xmlq, encoding='utf-8', method='xml').decode()).toprettyxml(encoding='utf8', indent='    '),
            parseString(ET.tostring(xmla, encoding='utf-8', method='xml').decode()).toprettyxml(encoding='utf8', indent='    '),
            parseString(ET.tostring(xmls, encoding='utf-8', method='xml').decode()).toprettyxml(encoding='utf8', indent='    ')
        )


if __name__ == '__main__':

    # Execute like so:
    # $ python catalog-json-to-xml.py AZF.pdf ../src/main/res/value/
    #   -> ../src/main/res/value/azf-questions.xml
    #   -> ../src/main/res/value/azf-answers.xml
    # $ python catalog-json-to-xml.py BZF.pdf ../src/main/res/value/


    parser = ArgumentParser()
    parser.add_argument('file')
    parser.add_argument('output')
    parser.add_argument('--prefix')
    parser.add_argument('--shuffle', action='store_true')

    args = parser.parse_args()
    if args.prefix is None:
        args.prefix = p.splitext(p.basename(args.file))[0].lower()

    questions, answers, solutions = convert(args.file, args.prefix, args.shuffle)
    path = p.dirname(args.file)
    with open(p.join(args.output, '{}_questions.xml'.format(args.prefix)), 'wb') as file:
        file.write(questions)

    with open(p.join(args.output, '{}_answers.xml'.format(args.prefix)), 'wb') as file:
        file.write(answers)

    with open(p.join(args.output, '{}_solutions.xml'.format(args.prefix)), 'wb') as file:
        file.write(solutions)

    print('All files writen to {}'.format(args.output))

