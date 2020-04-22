package demo;

public class Person {
    //:AUG_CODE: Snippets.generateClassContents
    //:JSON: {
    //:JSON:  "fields": [
    //:JSON: { "name": "name", "type": "String" },
    //:JSON: { "name": "age", "type": "int" }
    //:JSON: ]}
    //:GEN_CODE_START:
    private String name;
    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    //:GEN_CODE_END:
}