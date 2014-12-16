bmunit-tutorial2
================

This git repository contains a maven project which provides all the
code and Byteman rule scripts used for the advanced tutorial for
BMUnit, the package which integrates Byteman into JUnit and TestNG. It
provides a small application which employs a process dataflow pipeline
to execute a sequence of transformations on a stream of text bytes in
parallel.

The basic tests in the project show how you can use BMUnit to trace
and validate the data flowing through the private streams which link
each of the processes (threads) in the pipeline.

More complex tests engineer faults which exercise the fault handling
capabilities of the application. These tests inject errors into a
specific stag eof the processing pipeline. This is used to verify that
the threads in the pipeline recursively propagate the error up the
stream to ensure that upstream threads terminate. It also checks that
they close downstream links so that downstream threads also
terminate.

A full explanation of how the application and tests operate with
instructions on how to run both is provided in the tutorial at

    http://community.jboss.org/wiki/FaultInjectionTestingWithByteman#top

