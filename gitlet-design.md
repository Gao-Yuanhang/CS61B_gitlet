# Gitlet Design Document

**Name**:

## Classes and Data Structures

### Class 1

#### Fields

1. Field 1
2. Field 2


### Class 2

#### Fields

1. Field 1
2. Field 2


## Algorithms

## Persistence



测试

- 更换pwd 针对多个目录的操作
- 



每次调用只执行一个命令，也就是所有的信息 包括commit等都要序列化到gitlet中

可改进：递归执行 而不是单纯将文件夹看成一个文件对象



```
cd ./testing
D:\2024\CS61B projects\CS61B_gitlet\gitlet\target\classes\gitlet
.\gitlet\target\classes\gitlet
python tester.py --verbose test01-init.in
python tester.py --verbose --progdir=".\gitlet\target\classes\gitlet" test01-init.in


python tester.py --verbose --progdir="D:/2024/CS61B_projects/CS61B_gitlet/target/classes" D:/2024/CS61B_projects/CS61B_gitlet/testing/samples/test01-init.in

java -cp D:\2024\CS61B projects\CS61B_gitlet\gitlet\target gitlet.Main


python tester.py samples/test01-init.in
python tester.py samples/test02-basic-checkout.in //not available


java -cp D:\2024\CS61B_projects\CS61B_gitlet gitlet.Main
```



in the run configuration page, set working directory and debug



git commit message 规范

| Type       | 说明                         | 示例                                |
| ---------- | ---------------------------- | ----------------------------------- |
| `feat`     | 添加新功能                   | `feat(fwd): add forward pass logic` |
| `fix`      | 修复 bug                     | `fix(rev): correct gradient bug`    |
| `docs`     | 修改文档                     | `docs(readme): update usage guide`  |
| `style`    | 代码格式修改（不影响逻辑）   | `style(loma): reformat indentation` |
| `refactor` | 重构代码（无新增功能或修复） | `refactor(core): clean up loops`    |
| `perf`     | 性能优化                     | `perf(rev): speed up backprop`      |
| `test`     | 添加或修改测试               | `test(loma): add unit tests`        |
| `build`    | 构建系统变更（如 Makefile）  | `build(ci): add build step`         |
| `ci`       | 持续集成配置                 | `ci(github): update workflow`       |
| `chore`    | 杂项，不影响源代码或测试     | `chore(deps): update dependencies`  |
| `revert`   | 回滚提交                     | `revert: undo feat(rev) commit`     |



### scope 常见示例（括号中）：

- `api`：接口层
- `ui`：前端或界面
- `core`：核心逻辑
- `test`：测试模块
- `docs`：文档相关







当前仓库唯一的head 一定是当前分支的head

但分支没有之前所想象的重要/强大，分支（的head）本质只是一个指向commit的名字，是暂时的

但commit是一经提交不会改变的 除非由于没有东西（分支，tag，head）引用它导致丢失（一经离开，切换到其它commit就无法再找到这个commit）这个commit最终被垃圾回收

**track一个分支的历史是完全没有意义的**，对一个branch来说只有head是有用的，它曾经head指向过谁完全没有意义

在正常的提交路径下，一个分支的历史比如是从主干分出、单独开发、合并回去的清晰路径

但假如把当前分支的head指向一个与该分支毫无关系的commit上也完全没有问题

​	如main和dev分支的head为c1，c2，将main的head切换到c2，OK，当前分支还是main，进行一次新提交得到commit c3，此时当前分支为main，c3的parent是c2（commit tree是immutable的），当前分支的head是c3，这是完全合法的

但此时的问题是，原本head指向的c1没有被任何东西引用，它会被垃圾回收



















AI 和 autodriving的结合



























































