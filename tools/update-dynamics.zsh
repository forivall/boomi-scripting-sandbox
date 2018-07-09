#!/usr/bin/env zsh

S="${0:A}"
R="${S%/*/*}"

toStdout() {
<<ENDHEADER
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="DynamicElementsStorage">
    <option name="containingClasses">
      <map>
ENDHEADER
for f in $R/scripts/**.groovy ; do
fbase=${f##**/}
fkey=${fbase%.groovy}
<<ENDENTRY
        <entry key="$fkey">
          <value>
            <DClassElement>
              <option name="name" value="$fkey" />
              <option name="myName" value="$fkey" />
              <option name="myProperties">
                <set>
                  <DPropertyElement>
                    <option name="name" value="dataContext" />
                    <option name="static" value="false" />
                    <option name="type" value="com.boomi.document.scripting.DataContextImpl" />
                    <option name="myType" value="com.boomi.document.scripting.DataContextImpl" />
                    <option name="myStatic" value="false" />
                    <option name="myName" value="dataContext" />
                  </DPropertyElement>
                </set>
              </option>
            </DClassElement>
          </value>
        </entry>
ENDENTRY
done
<<ENDFOOTER
      </map>
    </option>
  </component>
</project>
ENDFOOTER
}

toStdout > $R/.idea/dynamic.xml