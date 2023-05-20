lab4:
	javac lab4.java
	javac Instruction.java

run_tests:
	java lab4 lab4_test1.asm lab4_test1.script > out1
	diff -w -B out1 lab4_test1.output

	java lab4 lab4_test2.asm lab4_test2.script > out2
	diff -w -B out2 lab4_test2.output

	java lab4 lab4_fib10.asm lab4_fib10.script > out3
	diff -w -B out3 lab4_fib10.output

	java lab4 lab4_fib20.asm lab4_fib20.script > out4
	diff -w -B out4 lab4_fib20.output
