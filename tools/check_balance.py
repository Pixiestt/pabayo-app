import sys
from pathlib import Path
p=Path(sys.argv[1])
s=p.read_text()
stack=[]
pairs={')':'(',']':'[','}':'{'}
line=1
col=1
first_error=None
for i,ch in enumerate(s):
    if ch=='\n':
        line+=1; col=1; continue
    if ch in '({[':
        stack.append((ch,line,col,i))
    elif ch in ')}]':
        if not stack or stack[-1][0]!=pairs[ch]:
            first_error=(line,col,ch,stack[-1] if stack else None)
            break
        stack.pop()
    col+=1
print('file:',p)
if first_error:
    print('first mismatch at line',first_error[0],'col',first_error[1],'found',first_error[2],'top of stack',first_error[3])
else:
    print('no bracket mismatch')
print('stack size',len(stack))
if stack:
    print('top of stack last 5:',stack[-5:])
# Also quick count of quotes
print('double quotes count', s.count('"'))
print('single quotes count', s.count("'"))
print('backticks count', s.count('`'))

