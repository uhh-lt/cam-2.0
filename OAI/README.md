# Objects and Aspects Identification Project

[![UHH](https://www.kus.uni-hamburg.de/5572339/uhh-logo-2010-29667bd15f143feeb1ebd96b06334fddfe378e09.png)](https://www.uni-hamburg.de/)<a href="https://www.inf.uni-hamburg.de/en/inst/ab/sems/home.html"><img src="https://www.inf.uni-hamburg.de/5546980/lt-logo-640x361-9345df620ffab7a8ce97149b66c2dfc9d3ff429e.png" width="200" height="100" /></a>


## Dataset

It uses the [Webis Comparative Questions 2022](https://zenodo.org/records/7213397) dataset. A split (train 70%, validation 9%, test 21%) from the file `comparative-questions-parsing/full.tsv` has been created.


## Setting up the environment

To set up the environment, you need to install poetry and run the following commands.
```bash
pipx install poetry
cd OAI/train
poetry install
```

 All the requirements are listed in `pyproject.toml`.

## Training

The encoder-based model can be trained on GPU by executing `train_bert.py`:

```bash
CUDA_VISIBLE_DEVICES=0 python train_bert.py
```

If you don't want to report to [WandB](https://wandb.ai/), please comment the `report_to` argument in TrainingArguments in `train_bert.py`.

## Hyperparameter Optimization

To optimize the hyperparameters of a new encoder-based model from HuggingFace, run `optimize_bert.py`:

```bash
CUDA_VISIBLE_DEVICES=0 python optimize_bert.py
```

## Cross Validation

To cross-validate a new encoder-based model from HuggingFace, run `cross_val_bert.py`:

```bash
CUDA_VISIBLE_DEVICES=0 python cross_val_bert.py
```

## Demo and API

Once the model is created, you can run a demo operated by [Gradio](https://www.gradio.app/):

```bash
python demo.py
```

The API was created to access the model through a request. It is in the main file `main.py`.

```bash
python main.py
```
