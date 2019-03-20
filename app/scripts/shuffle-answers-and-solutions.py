#!/usr/bin/env python3

import os
import os.path as p
import random
from xml.dom import minidom
import xml.etree.ElementTree as xml
from argparse import ArgumentParser

random.seed(42)  # to generate same results

dir = p.dirname(p.realpath(__file__))

parser = ArgumentParser('Reads questions.xml & answers.xml and writes shuffled answers.xml and corresponding solutions.xml')
parser.add_argument('--path', default=p.join(*'.. src main res values'.split()))
args = parser.parse_args()

paths = {
    'questions'   : p.join(dir, args.path, 'questions.xml'),
    'answers'     : p.join(dir, args.path, 'answers.xml'),
    'answers.bak' : p.join(dir, 'answers.bak.xml'),
    'solutions'   : p.join(dir, args.path, 'solutions.xml')
}

resources = {
    'questions': xml.parse(paths['questions']).getroot(),
    'answers':   xml.parse(paths['answers.bak']).getroot(),
    'solutions': xml.Element('resources')
}
questions = resources['questions'][0]
answers   = resources['answers'][0]
solutions = xml.SubElement(resources['solutions'], 'integer-array')
solutions.set('name', 'solutions')

if len(answers) / 4 != len(questions):
    raise Exception('Not enough/too many answers %s for amount of questions %s' % (answers.length/4, questions.length))

data = []
for i in range(0, len(answers), 4):
    idx = [0,1,2,3]
    random.shuffle(idx)
    solution = idx.index(0)
    xml.SubElement(solutions, 'item').text = str(solution)

    data.append([answers[i+j].text for j in idx])

seperator = xml.SubElement(resources['answers'], 'string')
seperator.set('name', 'answer_separator')
seperator.text=';'
answers.clear()
answers.set('formatted', 'false')
answers.set('name', 'answers')

for row in data:
    xml.SubElement(answers, 'item').text = u'A) {};\nB) {};\nC) {};\nD) {}'.format(*row)

for item in ['answers', 'solutions']:
    print('Writing file %s' % paths[item])
    with open(paths[item], 'wb') as file:
        file.write(xml.tostring(resources[item], encoding='utf-8'))
