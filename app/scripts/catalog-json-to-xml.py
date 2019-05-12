#!/usr/bin/env/python3

import json
import os
import os.path as p
from xml.dom.minidom import parseString
import xml.etree.ElementTree as ET
from argparse import ArgumentParser


def convert(file, prefix):
    with open(file) as f:
        catalog = json.load(f)

        xmlq = ET.Element('resources')
        xmla = ET.Element('resources')
        questions = ET.SubElement(xmlq, 'string-array')
        questions.set('name', '{}_questions'.format(prefix))
        answers = ET.SubElement(xmla, 'string-array')
        answers.set('name', '{}_answers'.format(prefix))
        answers.set('formatted', 'false')

        for item in catalog:
            # Error Checking
            for key in ['answers', 'question']:
                if key not in item:
                    raise ValueError('No "{}" field found in JSON file'.format(key))

            if len(item['answers']) != 4:
                raise ValueError('Question {} has not 4 but {} answers.'.format(item['question']['number'], len(item['answers'])))

            sep = ';' + os.linesep
            answer = ET.Element('item')
            answer.text = sep.join(map(lambda a: '{}) {}'.format(a['letter'], a['text']), item['answers']))
            answers.append(answer)

            question = ET.Element('item')
            question.text = '{}. {}'.format(item['question']['number'], item['question']['text'])
            questions.append(question)

        return (
            parseString(ET.tostring(xmlq, encoding='utf-8', method='xml').decode()).toprettyxml(encoding='utf8', indent='    '),
            parseString(ET.tostring(xmla, encoding='utf-8', method='xml').decode()).toprettyxml(encoding='utf8', indent='    ')
        )


if __name__ == '__main__':

    # Execute like so:
    # $ python catalog-json-to-xml.py AZF.pdf ../src/main/res/value/
    #   -> ../src/main/res/value/azf-questions.xml
    #   -> ../src/main/res/value/azf-answers.xml
    # $ python catalog-json-to-xml.py BZF.pdf ../src/main/res/value/

    # you want to shuffle-answers-and-solutions.py afterwards

    parser = ArgumentParser()
    parser.add_argument('file')
    parser.add_argument('output')
    parser.add_argument('--prefix')

    args = parser.parse_args()
    if args.prefix is None:
        args.prefix = p.splitext(p.basename(args.file))[0].lower()

    questions, answers = convert(args.file, args.prefix)
    path = p.dirname(args.file)
    with open(p.join(args.output, '{}_questions.xml'.format(args.prefix)), 'wb') as file:
        file.write(questions)

    with open(p.join(args.output, '{}_answers.xml'.format(args.prefix)), 'wb') as file:
        file.write(answers)

    print('All files writen to {}'.format(args.output))

