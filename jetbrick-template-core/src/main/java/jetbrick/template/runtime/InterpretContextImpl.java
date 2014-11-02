/**
 * Copyright 2013-2014 Guoqiang Chen, Shanghai, China. All rights reserved.
 *
 *   Author: Guoqiang Chen
 *    Email: subchen@gmail.com
 *   WebURL: https://github.com/subchen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrick.template.runtime;

import java.util.*;
import jetbrick.template.*;
import jetbrick.template.parser.Source;
import jetbrick.template.resolver.GlobalResolver;

/**
 *  InterpretContext 默认实现
 */
public final class InterpretContextImpl extends InterpretContext {
    private final JetEngine engine;
    private final GlobalResolver globalResolver;
    private final TemplateStack templateStack;
    private final ValueStack valueStack;
    private JetWriter writer;
    private int signal;
    private String returnName;

    public InterpretContextImpl(JetEngine engine, JetWriter writer, Map<String, Object> context) {
        this.engine = engine;
        this.globalResolver = engine.getGlobalResolver();
        this.templateStack = new TemplateStack();
        this.valueStack = new ValueStack(engine.getGlobalContext(), context);
        this.writer = writer;
        this.signal = InterpretContext.SIGNAL_NONE;
        threadLocal.set(this);
    }

    public void remove() {
        threadLocal.remove();
    }

    @Override
    public JetEngine getEngine() {
        return engine;
    }

    @Override
    public GlobalResolver getGlobalResolver() {
        return globalResolver;
    }

    @Override
    public TemplateStack getTemplateStack() {
        return templateStack;
    }

    @Override
    public ValueStack getValueStack() {
        return valueStack;
    }

    @Override
    public JetTemplate getTemplate() {
        return templateStack.current();
    }

    @Override
    public Source getSource() {
        return templateStack.current().getSource();
    }

    @Override
    public JetWriter getWriter() {
        return writer;
    }

    @Override
    public void setWriter(JetWriter writer) {
        this.writer = writer;
    }

    @Override
    public int getSignal() {
        return signal;
    }

    @Override
    public void setSignal(int signal) {
        this.signal = signal;
    }

    @Override
    public String getReturnName() {
        return returnName;
    }

    @Override
    public void doIncludeCall(String file, Map<String, Object> arguments, String returnName) {
        JetTemplate template = engine.getTemplate(file);

        this.returnName = returnName; // use new name

        templateStack.push(template);
        valueStack.push(template.getOption().getSymbols(), arguments, true);

        template.getAstNode().execute(this);

        valueStack.pop();
        templateStack.pop();

        this.returnName = null; // clear
    }

    @Override
    public void doMacroCall(JetTemplateMacro macro, Object[] arguments) {
        List<String> names = macro.getArgumentNames();
        if (names.size() != arguments.length) {
            throw new IllegalArgumentException("macro arguments do not match: " + macro.getName());
        }

        boolean isCrossTemplate = (templateStack.current() != macro.getTemplate());
        if (isCrossTemplate) {
            // 检测 inlcude 文件是否被修改
            macro.getTemplate().reload();
            templateStack.push(macro.getTemplate());
        }

        Map<String, Object> args;
        if (arguments == null || arguments.length == 0) {
            args = null;
        } else {
            args = new HashMap<String, Object>();
            for (int i = 0; i < arguments.length; i++) {
                args.put(names.get(i), arguments[i]);
            }
        }
        valueStack.push(macro.getSymbols(), args, true);

        macro.getAstNode().execute(this);

        valueStack.pop();

        if (isCrossTemplate) {
            templateStack.pop();
        }
    }

}