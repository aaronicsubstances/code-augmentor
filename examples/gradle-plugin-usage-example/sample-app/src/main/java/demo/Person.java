package demo;

public class Person {
    //:AUG_CODE: Snippets.generateClassContents
    //:JSON: {
    //:JSON:  "fields": [
    //:JSON: { "name": "name", "type": "String" },
    //:JSON: { "name": "age", "type": "Integer" }
    //:JSON: ]}
    //:GEN_CODE_START:
    private String name;
    private Integer age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    //:GEN_CODE_END:
}