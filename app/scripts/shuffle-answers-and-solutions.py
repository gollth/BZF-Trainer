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
group = parser.add_mutually_exclusive_group(required=True)
group.add_argument('--azf', dest='cat', action='store_const', const='azf')
group.add_argument('--bzf', dest='cat', action='store_const', const='bzf')
args = parser.parse_args()

paths = {
    'questions'   : p.join(dir, args.path, f'{args.cat}_questions.xml'),
    'answers'     : p.join(dir, args.path, f'{args.cat}_answers.xml'),
    'original'    : p.join(dir, args.path, f'{args.cat}_original.xml'),
    'solutions'   : p.join(dir, args.path, f'{args.cat}_solutions.xml')
}

resources = {
    'questions': xml.parse(paths['questions']).getroot(),
    'answers':   xml.parse(paths['original']).getroot(),
    'solutions': xml.Element('resources')
}
questions = resources['questions'][0]
answers   = resources['answers'][0]
solutions = xml.SubElement(resources['solutions'], 'integer-array')
solutions.set('name', f'{args.cat}_solutions')

if len(answers) / 4 != len(questions):
    raise Exception('Not enough/too many answers %s for amount of questions %s' % (len(answers)/4, len(questions)))

data = []
for i in range(0, len(answers), 4):
    idx = [0,1,2,3]
    random.shuffle(idx)
    solution = idx.index(0)
    xml.SubElement(solutions, 'item').text = str(solution)

    data.append([answers[i+j].text for j in idx])

answers.clear()
answers.set('formatted', 'false')
answers.set('name', f'{args.cat}_answers')

for row in data:
    xml.SubElement(answers, 'item').text = u'{};\n{};\n{};\n{}'.format(*row)

for item in ['answers', 'solutions']:
    print('Writing file %s' % paths[item])
    with open(paths[item], 'wb') as file:
        file.write(xml.tostring(resources[item], encoding='utf-8'))

print('Dont forget to auto-format the files in Android Studio with CTRL+ALT+L / OPTION+CMD+L !!!')
