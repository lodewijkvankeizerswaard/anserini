#!/bin/sh
mvn clean package appassembler:assemble -DadditionalJOption=-Xdoclint:none
